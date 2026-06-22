package it.polimi.ingsw.controller;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.GameObserver;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.cards.TribeCard;
import it.polimi.ingsw.model.cards.Buildings.BuildingEachTurn;
import it.polimi.ingsw.model.cards.Buildings.BuildingEnd;
import it.polimi.ingsw.model.cards.Buildings.BuildingEvent;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.database.DatabaseManager;
import it.polimi.ingsw.database.RankingEntry;
import it.polimi.ingsw.persistence.PersistenceManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Server-side MVC controller.
 *
 * - Holds the nickname → VirtualView map
 * - Receives client actions, validates them, and delegates them to the model (Game class)
 * - Implements GameObserver: translates model notifications into callbacks to clients
 * - buildExtraInfo() builds the "info panel" sent to the current player (better here for direct connection to the Player class)
 */

public class GameController implements GameObserver {

    private final Game game;
    private final List<VirtualView> clients = new CopyOnWriteArrayList<>();
    private final List<String> nicks = new CopyOnWriteArrayList<>();
    private int expectedPlayers = -1;

    /** True while the server waits for players to reconnect after a crash. */
    private boolean recovering = false;

    /** True when all real players have disconnected — bots stop acting. */
    private volatile boolean gameAborted = false;

    /** Nicknames of players currently replaced by a bot (both during gameplay and recovery). */
    private final Set<String> botNicknames = new HashSet<>();

    /*
    // === SPECTATOR ===
    private final List<VirtualView> spectators = new CopyOnWriteArrayList<>();
    private final List<String> spectatorNicks = new CopyOnWriteArrayList<>();
    // === END SPECTATOR ===

     */

    private static final TotemColor[] TOTEM_COLORS = TotemColor.values();

    public GameController(Game game) {
        this.game = game;
        game.addObserver(this);
    }

    /**
     * Constructor for games restored from disk: {@code recovering=true} blocks auto-save
     * and waits for all players to reconnect before resuming.
     *
     * @param game       the restored game model
     * @param recovering whether the game is in recovery mode
     */
    public GameController(Game game, boolean recovering) {
        this.game = game;
        this.expectedPlayers = game.getNumberOfPlayers();
        this.recovering = recovering;
        game.addObserver(this);
    }

    /**
     * Constructor for games restored from disk that had active bots.
     * Nicknames in {@code savedBotNicknames} do not need to reconnect: they are automatically
     * recreated as {@link Bot} instances once all real players have returned.
     *
     * @param game              the restored game model
     * @param recovering        whether the game is in recovery mode
     * @param savedBotNicknames nicknames that were represented by bots at crash time
     */
    public GameController(Game game, boolean recovering, List<String> savedBotNicknames) {
        this.game = game;
        this.expectedPlayers = game.getNumberOfPlayers();
        this.recovering = recovering;
        if (savedBotNicknames != null) this.botNicknames.addAll(savedBotNicknames);
        game.addObserver(this);
    }

    // ================================================================== //
    //  LOBBY STATE
    // ================================================================== //

    /**
     * True if the lobby is still accepting players (expectedPlayers not yet
     * reached and the game has not yet started).
     * synchronized: isOpen() reads fields written by loginFirstPlayer() (synchronized).
     */
    public synchronized boolean isOpen() {
        return expectedPlayers == -1
                || (game.getNumberOfPlayers() < expectedPlayers
                && game.getStatus() == GameState.LOGIN);
    }


    /** Returns true if this game has finished (reached the END state). */
    public synchronized boolean isFinished() {
        return game.getStatus() == GameState.END;
    }

    /** Returns true if the game is running and is no longer accepting new players. */
    public synchronized boolean isInProgress() {
        return !isOpen() && !isFinished();
    }

    /** Returns the number of players currently registered in this lobby. */
    public synchronized int getCurrentPlayers()  { return game.getNumberOfPlayers(); }

    /** Returns the total number of players this lobby is configured to accept. */
    public int getExpectedPlayers() { return expectedPlayers; }

    /**
     * Returns true if a player with the given nickname is registered in this lobby.
     *
     * @param nickname the player's nickname
     * @return true if the nickname is present in the player list
     */
    public boolean hasPlayer(String nickname) {
        return nicks.contains(nickname);
    }

    /** Returns true if the game is in recovery mode (waiting for players to reconnect). */
    public boolean isRecovering() { return recovering; }

    /** Returns true if the game has been aborted due to all players disconnecting. */
    public boolean isAborted()    { return gameAborted; }

    /**
     * Returns true if the restored game model contains a player with this nickname
     * and that player was not a bot at the time of the crash (bots reconnect automatically).
     *
     * @param nickname the player's nickname to look up
     * @return true if the nickname belongs to a real player in the saved game
     */
    public boolean hasPlayerInGame(String nickname) {
        return game.getPlayers().stream().anyMatch(p -> p.getNickname().equals(nickname))
                && !botNicknames.contains(nickname);
    }

    /**
     * Reconnects a client that is rejoining after a server crash.
     * When all expected players have reconnected, resumes the game by broadcasting
     * {@code onGameStarting} and triggering the current player's turn.
     *
     * @param nickname the reconnecting player's nickname
     * @param caller   the player's VirtualView (new network connection)
     */
    public synchronized void reconnectPlayer(String nickname, VirtualView caller) {
        clients.add(caller);
        nicks.add(nickname);
        try { caller.onLoginAccepted(nickname, expectedPlayers); } catch (Exception ignored) {}

        // Real players to wait for are those who were not bots at the time of the crash.
        int realExpected = expectedPlayers - botNicknames.size();
        System.out.println("[GameController] Reconnected: " + nickname
                + " (" + nicks.size() + "/" + realExpected + " reali, "
                + botNicknames.size() + " bot da respawnare)");

        if (nicks.size() == realExpected) {
            // All real players are back — recreate bots for the missing slots.
            for (String botNick : botNicknames) {
                Bot bot = new Bot(botNick, this, game);
                clients.add(bot);
                nicks.add(botNick);
                System.out.println("[GameController] Bot ricreato per: " + botNick);
            }

            recovering = false;
            List<String> allNicks = game.getPlayers().stream()
                    .map(Player::getNickname).toList();
            broadcast(v -> v.onGameStarting(allNicks));
            Player cur = game.getCurrentPlayer();
            if (cur != null) {
                onTurnStarted(cur.getNickname(), game.getStatus());
            }
        }
    }

    /*
    // === SPECTATOR ===
    //True if a spectator with that nickname is registered in this lobby.
    public boolean hasSpectator(String nickname) {
        return spectatorNicks.contains(nickname);
    }

    /**
     * Adds a spectator: receives all public broadcast events but never onYourTurn.
     * Sends an immediate board snapshot so the spectator sees the current state.

    public void addSpectator(String nick, VirtualView view) {
        spectators.add(view);
        spectatorNicks.add(nick);
        // Build snapshot synchronously, then send it in a separate thread.
        // This prevents the caller (LobbyManager, which is synchronized) from
        // holding its lock while waiting on a blocking network call → no deadlock.
        Player current = game.getCurrentPlayer();
        String currentNick = (current != null) ? current.getNickname() : "";
        String snapshot = buildBoardSummaryForWatchers();
        new Thread(() -> {
            try {
                view.onSpectatorJoined(currentNick, snapshot);
            } catch (Exception e) {
                System.err.println("[GameController] addSpectator initial snapshot: " + e.getMessage());
            }
        }, "spectator-snapshot-" + nick).start();
    }

    /** Removes a spectator by view reference.
    public void removeSpectator(String nick) {
        int idx = spectatorNicks.indexOf(nick);
        if (idx >= 0) {
            spectators.remove(idx);
            spectatorNicks.remove(idx);
        }
    }
    // === END SPECTATOR ===
    */


    // ─────────────────────────────────────────────────────────────────────
    //  LOBBY
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Creates a new lobby and registers the first player.
     *
     * @param nickname   the creating player's nickname
     * @param numPlayers the total number of players expected (2–5)
     * @param caller     the creating player's VirtualView
     */
    public synchronized void loginFirstPlayer(String nickname, int numPlayers, VirtualView caller) {
        try {
            if (numPlayers < 2 || numPlayers > 5) {
                caller.onError("Numero di giocatori non valido (2-5).");
                return;
            }
            if (expectedPlayers != -1) {
                caller.onError("Lobby già creata.");
                return;
            }

            expectedPlayers = numPlayers;
            registerClient(nickname, caller);
            caller.onLoginAccepted(nickname, expectedPlayers);

            broadcastPlayerJoined(nickname);
            checkAndStartIfReady();

        } catch (Exception e) {
            System.err.println("[Controller] loginFirstPlayer: " + e.getMessage());
        }
    }

    /**
     * Registers an additional player in an existing lobby.
     *
     * @param nickname the joining player's nickname
     * @param caller   the joining player's VirtualView
     */
    public synchronized void login(String nickname, VirtualView caller) {
        try {
            if (expectedPlayers == -1) {
                caller.onError("Nessuna lobby attiva.");
                return;
            }
            if (game.getNumberOfPlayers() >= expectedPlayers) {
                caller.onError("Lobby piena.");
                return;
            }
            if (nicks.contains(nickname)) {
                caller.onError("Nickname '" + nickname + "' già in uso.");
                return;
            }
            registerClient(nickname, caller);
            caller.onLoginAccepted(nickname, expectedPlayers);
            broadcastPlayerJoined(nickname);
            checkAndStartIfReady();
        } catch (Exception e) {
            System.err.println("[Controller] login: " + e.getMessage());
        }
    }

    private void registerClient(String nickname, VirtualView caller) {
        TotemColor color = TOTEM_COLORS[game.getNumberOfPlayers() % TOTEM_COLORS.length]; //color choose - random

        game.addPlayer(new Player(nickname, new Totem(color)));

        clients.add(caller);
        nicks.add(nickname);
    }

    private void broadcastPlayerJoined(String newNick) throws Exception {
        int cur = game.getNumberOfPlayers(), exp = expectedPlayers;
        for (VirtualView v : clients) v.onPlayerJoined(newNick, cur, exp);
    }

    private void checkAndStartIfReady() throws Exception {
        if (game.getNumberOfPlayers() < expectedPlayers) return;

        List<String> allNicks = game.getPlayers().stream().map(Player::getNickname).toList();
        System.out.println("\n=== INIZIO PARTITA === " + allNicks);

        for (VirtualView v : clients) v.onGameStarting(allNicks);

        game.startGame(); // → notifyCurrentPlayerTurn() → onTurnStarted()
    }


    // ─────────────────────────────────────────────────────────────────────
    //  ACTIONS CLIENT → GAME
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Handles a player's request to place their totem on an offer space.
     *
     * @param nickname the acting player's nickname
     * @param letter   the letter identifying the target offer space
     */
    public synchronized void placeTotem(String nickname, char letter) {
        try {
            Player player = findPlayer(nickname);
            if (player == null) { safeError(nickname, "Giocatore non trovato."); return; }

            if (!isPlayerTurn(nickname)) {
                safeError(nickname, "Non è il tuo turno.");
                return;
            }
            if (game.getStatus() != GameState.OFFER_SPACE_CHOOSE) {
                safeError(nickname, "Non siamo nella fase di piazzamento totem.");
                return;
            }

            BoardSpace space = game.getBoard().getBoardSpace(letter);
            if (space == null) {
                safeInvalidAction(nickname, "Spazio '" + letter + "' non esiste o non è in gioco.");
                return;
            }

            game.placeTotemOnOfferSpace(player, space);
            // → Game notifica onTotemPlaced (broadcast) + onTurnStarted (al prossimo giocatore)

        } catch (IllegalStateException e) {
            safeInvalidAction(nickname, e.getMessage());
        } catch (Exception e) {
            System.err.println("[Controller] placeTotem: " + e.getMessage());
        }
    }

    /**
     * Handles a player's request to pick a card from one of the board rows.
     *
     * @param nickname  the acting player's nickname
     * @param cardIndex 0-based index of the card within the chosen row
     * @param fromTop   true to pick from the top row, false for the bottom row
     */
    public synchronized void pickCard(String nickname, int cardIndex, boolean fromTop) {
        try {
            Player player = findPlayer(nickname);
            if (player == null) { safeError(nickname, "Giocatore non trovato."); return; }

            if (!isPlayerTurn(nickname)) {
                safeError(nickname, "Non è il tuo turno.");
                return;
            }
            if (game.getStatus() != GameState.PICKING_CARD) {
                safeError(nickname, "Non siamo nella fase di selezione carte.");
                return;
            }

            List<? extends Card> available;
            if (fromTop) {
                List<Card> top = new ArrayList<>();
                top.addAll(game.getBoard().getAvailableUpperTribeCards());
                top.addAll(game.getBoard().getAvailableUpperBuildingCards());
                available = top;
            } else {
                List<Card> bot = new ArrayList<>();
                bot.addAll(game.getBoard().getAvailableBottomTribeCards());
                bot.addAll(game.getBoard().getAvailableBottomBuildingCards());
                available = bot;
            }

            if (cardIndex < 0 || cardIndex >= available.size()) {
                safeInvalidAction(nickname, "Indice carta non valido (0–" + (available.size() - 1) + ").");
                return;
            }

            Card card = available.get(cardIndex);

            // ── Guard: event cards sit in the tribe row but cannot be picked by players.
            if (card instanceof EventCard) {
                safeInvalidAction(nickname,
                        "La carta [" + card + "] è una carta Evento: non può essere pescata. "
                                + "Scegli un'altra carta.");
                return;
            }

            game.pickCard(player, card);
            // → Game notify onCardTaken + onPlayerUpdated
            // → if no more card → returnTotemToTurnOrder + advanceNextPlayer
            // → if no more player to pick → resolveEvents → nextRound / endGame

        } catch (IllegalStateException | IllegalArgumentException e) {
            // Both exception types signal a rule violation — send it to the client as a retryable error.
            safeInvalidAction(nickname, e.getMessage());
        } catch (Exception e) {
            System.err.println("[Controller] pickCard: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GameObserver — model notify → client callback
    // ─────────────────────────────────────────────────────────────────────

    /** No-op: player join notifications are managed directly in {@link #broadcastPlayerJoined}. */
    @Override public void onPlayerJoined(String nickname) { /* managed in broadcastPlayerJoined */ }

    /**
     * Forwards a model-level error to the currently active player.
     *
     * @param message the error message to deliver
     */
    @Override
    public void onPlayerError(String message) {
        try {
            Player current = game.getCurrentPlayer();
            if (current != null) {
                safeError(current.getNickname(), message);
            }
        } catch (Exception e) {
            System.err.println("[Controller] onPlayerError: " + e.getMessage());
        }
    }

    /** No-op: game start is broadcast directly in {@link #checkAndStartIfReady}. */
    @Override public void onGameStarted() { /* managed in checkAndStartIfReady */ }

    /**
     * The model tells us who must act and in which phase.
     *
     * Strategy:
     *  1. Build a full board snapshot and broadcast it to ALL non-active players via onTurnSnapshot.
     *     This keeps every waiting player informed of the current game state.
     *  2. Send the detailed action panel (available cards, player's own hand) only to the active player.
     */
    @Override
    public void onTurnStarted(String nickname, GameState phase) {
        try {
            // ── 1. Board snapshot → all waiting players ───────────────────
            String boardSummary = buildBoardSummaryForWatchers();
            for (int i = 0; i < nicks.size(); i++) {
                if (!nicks.get(i).equals(nickname)) {
                    try { clients.get(i).onTurnSnapshot(nickname, boardSummary); }
                    catch (Exception e) {
                        System.err.println("[Controller] onTurnSnapshot → " + nicks.get(i) + ": " + e.getMessage());
                    }
                }
            }

            /*
            // === SPECTATOR: snapshot sent to all spectators at every turn ===
            for (VirtualView spectator : spectators) {
                try { spectator.onTurnSnapshot(nickname, boardSummary); }
                catch (Exception e) {
                    System.err.println("[Controller] onTurnSnapshot → spectator: " + e.getMessage());
                }
            }
            // === END SPECTATOR ===

             */

            // ── 2. Full action panel → active player only ─────────────────
            VirtualView target = viewOf(nickname);
            if (target == null) return;
            String extra = buildExtraInfo(nickname, phase);
            target.onYourTurn(nickname, phase, extra);

        } catch (Exception e) {
            System.err.println("[Controller] onTurnStarted: " + e.getMessage());
        }

        // Persistence: save after each turn change (skipped during recovery).
        if (!recovering) PersistenceManager.getInstance().save(game, botNicknames);
    }

    /**
     * Broadcasts a totem-placed event to all clients.
     *
     * @param nickname    the player who placed the totem
     * @param boardSpaceId identifier of the offer space where the totem was placed
     */
    @Override
    public void onTotemPlaced(String nickname, String boardSpaceId) {
        broadcast(v -> v.onTotemPlaced(nickname, boardSpaceId));
    }

    /**
     * Forwards an invalid-action notification to the specific target player.
     *
     * @param nicknameTarget the player who attempted the invalid action
     * @param errorMessage   description of why the action was rejected
     */
    @Override
    public void onInvalidAction(String nicknameTarget, String errorMessage) {
        try {
            VirtualView t = viewOf(nicknameTarget);
            if (t != null) t.onInvalidAction(nicknameTarget, errorMessage);
        } catch (Exception e) {
            System.err.println("[Controller] onInvalidAction: " + e.getMessage());
        }
    }

    /**
     * Broadcasts a card-taken event to all clients.
     *
     * @param nickname the player who picked the card
     * @param cardId   identifier of the card that was taken
     */
    @Override
    public void onCardTaken(String nickname, String cardId) {
        broadcast(v -> v.onCardTaken(nickname, cardId));
    }

    /**
     * Broadcasts a player-updated event so all clients can refresh the player's stats.
     *
     * @param nickname the player whose state changed
     */
    @Override
    public void onPlayerUpdated(String nickname) {
        broadcast(v -> v.onPlayerUpdated(nickname));
    }

    /**
     * Broadcasts the new turn order to all clients after totems are returned.
     *
     * @param ordered list of player nicknames in the new turn order
     */
    @Override
    public void onTurnOrderUpdated(List<String> ordered) {
        broadcast(v -> v.onTurnOrderUpdated(ordered));
    }

    /**
     * Broadcasts an event-resolved notification with an animation payload to all clients.
     *
     * @param eventName name of the event that was resolved
     * @param details   raw detail string from the model, enriched with per-player stats
     */
    @Override
    public void onEventResolved(String eventName, String details) {
        String payload = buildEventAnimationPayload(details);
        broadcast(v -> v.onEventResolved(eventName, payload));
    }

    /** Broadcasts a board-updated notification to all clients. */
    @Override
    public void onBoardUpdated() {
        broadcast(v -> v.onBoardUpdated());
    }

    /**
     * Broadcasts a new-era notification and persists the updated game state.
     *
     * @param newEra the era that has just started
     */
    @Override
    public void onNewEraStarted(Age newEra) {
        broadcast(v -> v.onNewEraStarted(newEra));
        if (!recovering) PersistenceManager.getInstance().save(game, botNicknames);
    }

    @Override
    public void onGameOver() {
        String results = buildFinalResults();
        broadcast(v -> v.onGameOver(results));

        // FA1: save to DB and send individual ranking to each player (not spectators)
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            int numPlayers = game.getNumberOfPlayers();
            db.saveGameResults(game.getPlayers(), numPlayers, LocalDate.now());
            List<RankingEntry> ranking = db.getRanking(numPlayers);

            for (int i = 0; i < clients.size(); i++) {
                String nick = nicks.get(i);
                VirtualView v = clients.get(i);
                int myScore = game.getPlayers().stream()
                        .filter(p -> p.getNickname().equals(nick))
                        .mapToInt(Player::getTotalPoints)
                        .findFirst().orElse(0);
                int myRank = db.getPlayerRank(nick, myScore, numPlayers);
                try {
                    v.onRankingData(myRank, ranking.size(), ranking);
                } catch (Exception e) {
                    System.err.println("[Controller] onRankingData to " + nick + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[DB] Ranking unavailable: " + e.getMessage());
        }

        // Persistence: game ended normally — delete the save file.
        PersistenceManager.getInstance().delete(game.getGameID());
    }

    /**
     * Called by LobbyManager when a client connection drops unexpectedly.
     *
     * If real players remain, the disconnected player's VirtualView is replaced
     * by a BotVirtualView that continues the game with random choices.
     * If NO real players remain, the game is marked as aborted and the lobby
     * will be removed by LobbyManager.
     */
    public synchronized void onPlayerDisconnected(String nickname) {
        System.out.println("[GameController] Player disconnected: " + nickname);

        int idx = nicks.indexOf(nickname);
        if (idx < 0) {
            // Player not in the active list (spectator path already handled by LobbyManager).
            broadcast(v -> v.onPlayerDisconnected(nickname));
            return;
        }

        // If the player is already represented by a Bot, this notification is stale:
        // it comes from a new connection created by the reconnect loop (which did not find a
        // recovering game and opened a new lobby instead). Ignore to avoid spurious abort.
        if (clients.get(idx) instanceof Bot) {
            System.out.println("[GameController] Player " + nickname
                    + " è già un bot — notifica stale ignorata.");
            return;
        }

        // Count real (non-bot) clients that will remain after this disconnection.
        long realRemaining = countRealClients() - 1;

        if (realRemaining <= 0) {
            // No real player left — abort the game without spawning a bot.
            System.out.println("[GameController] All players disconnected — aborting game " + game.getGameID());
            broadcast(v -> v.onPlayerDisconnected(nickname));
            gameAborted = true;
            PersistenceManager.getInstance().delete(game.getGameID()); // no players left — clean up the save file
            return;
        }

        // Replace the disconnected player's VirtualView with a bot.
        Bot bot = new Bot(nickname, this, game);
        clients.set(idx, bot);
        botNicknames.add(nickname);  // track this slot as a bot for persistence

        broadcast(v -> v.onPlayerDisconnected(nickname));    // "X has disconnected"
        broadcast(v -> v.onPlayerReplacedByBot(nickname));   // "X will be replaced by a bot"

        // If it was the disconnected player's turn, re-trigger the turn for the bot.
        Player cur = game.getCurrentPlayer();
        if (cur != null && cur.getNickname().equals(nickname)) {
            onTurnStarted(nickname, game.getStatus());
        }
    }

    /**
     * Forces the end of a bot's turn when no valid card pick is available.
     * Package-private so BotVirtualView (same package) can call it.
     *
     * Replicates the exact advancement logic that Game.pickCard() executes
     * after the last pick: returnTotemToTurnOrder → advanceNextPlayer →
     * resolveEvents (if needed) → notify next player.
     * All three Game methods are public, so Game.java is not modified.
     */
    synchronized void skipBotPick(String nickname) {
        Player player = findPlayer(nickname);
        if (player == null) return;
        game.returnTotemToTurnOrder(player);
        game.advanceNextPlayer();
        if (game.getStatus() == GameState.EVENTS) {
            game.resolveEvents();
            return;
        }
        Player next = game.getCurrentPlayer();
        if (next != null) onTurnStarted(next.getNickname(), game.getStatus());
    }

    /** Number of connected clients that are real players (not BotVirtualView instances). */
    private long countRealClients() {
        return clients.stream().filter(v -> !(v instanceof Bot)).count();
    }

    /**
     * Returns true if the player with the given nickname is currently represented by a {@link Bot}
     * rather than a real network client.
     * Used by LobbyManager to prefer lobbies where the player is still a real client.
     *
     * @param nickname the player's nickname
     * @return true if a {@link Bot} is acting on behalf of this nickname
     */
    public synchronized boolean isBotPlayer(String nickname) {
        int idx = nicks.indexOf(nickname);
        return idx >= 0 && clients.get(idx) instanceof Bot;
    }


    // ─────────────────────────────────────────────────────────────────────
    // building extrainformations panel - for TUI and GUI - implemented here for better connection with players info
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Builds the multi-line text sent to the active player.
     *
     * OFFER_SPACE_CHOOSE phase:
     *   • Full board (card rows + offer spaces + free spaces)
     *   • All players' status
     *   • Player's own hand (characters + buildings)
     *
     * PICKING_CARD phase:
     *   • Remaining picks (top/bottom)
     *   • Available cards per row (only rows where picks remain)
     *   • All players' status
     *   • Player's own hand
     *   • Machine markers ##HAS_TOP## / ##HAS_BOT## for CLI parsing
     */
    private String buildExtraInfo(String nickname, GameState phase) {
        Player player = findPlayer(nickname);
        if (player == null) return "";

        StringBuilder sb = new StringBuilder();

        appendDeckStatus(sb);

        if (phase == GameState.OFFER_SPACE_CHOOSE) {
            appendBoardDisplay(sb);
            appendPlayersSummary(sb);
            appendAllPlayersCardsSummary(sb);
            appendPlayerCards(sb, player);

        } else if (phase == GameState.PICKING_CARD) {
            int remTop = game.getRemainingTopPicks(player);
            int remBot = game.getRemainingBottomPicks(player);

            sb.append("  Pescate rimaste → sopra: ").append(remTop)
                    .append("  |  sotto: ").append(remBot).append("\n");
            sb.append("  Il tuo cibo: ").append(player.getFood())
                    .append("  |  Prestige: ").append(player.getPrestige()).append("\n");
            sb.append("\n");

            if (remTop > 0) {
                sb.append("##HAS_TOP##");   // machine-readable marker for CLIView
                sb.append("  ┌── Riga SUPERIORE: ──────────────────────────────────────────────┐\n");
                List<Card> topCards = new ArrayList<>();
                topCards.addAll(game.getBoard().getAvailableUpperTribeCards());
                topCards.addAll(game.getBoard().getAvailableUpperBuildingCards());
                for (int i = 0; i < topCards.size(); i++) {
                    Card c = topCards.get(i);
                    sb.append(String.format("  │  [%d] %-49s (CardId: %d)%n", i, c.toDisplayString(), c.getID()));
                }
                sb.append("  └──────────────────────────────────────────────────────────────────┘\n");
            }

            if (remBot > 0) {
                sb.append("##HAS_BOT##");   // machine-readable marker for CLIView
                sb.append("  ┌── Riga INFERIORE: ──────────────────────────────────────────────┐\n");
                List<Card> botCards = new ArrayList<>();
                botCards.addAll(game.getBoard().getAvailableBottomTribeCards());
                botCards.addAll(game.getBoard().getAvailableBottomBuildingCards());
                for (int i = 0; i < botCards.size(); i++) {
                    Card c = botCards.get(i);
                    sb.append(String.format("  │  [%d] %-49s (CardId: %d)%n", i, c.toDisplayString(), c.getID()));
                }
                sb.append("  └──────────────────────────────────────────────────────────────────┘\n");
            }

            appendPlayersSummary(sb);
            appendAllPlayersCardsSummary(sb);
            appendPlayerCards(sb, player);
        }

        return sb.toString();
    }

    /**
     * Builds a concise board snapshot for all waiting players.
     * Contains: card rows, offer spaces, player status.
     */
    private String buildBoardSummaryForWatchers() {
        StringBuilder sb = new StringBuilder();

        // include a deck status snapshot — the GUI uses it to keep era, card back, and remaining count in sync
        appendDeckStatus(sb);

        appendBoardDisplay(sb);
        appendPlayersSummary(sb);
        appendAllPlayersCardsSummary(sb);
        return sb.toString();
    }

    /**
     * Appends a machine-readable block with the current deck status.
     * Kept separate from human-readable text so the GUI can parse it robustly
     * without fragile parsing of game labels or descriptions.
     *
     * @param sb the target StringBuilder
     */
    private void appendDeckStatus(StringBuilder sb) {
        sb.append("##DECK_STATUS_BEGIN##\n");
        sb.append("AGE=").append(game.getCurrentAge().name()).append("\n");
        sb.append("REMAINING=").append(game.getRemainingCardsInTotalDeck()).append("\n");
        sb.append("##DECK_STATUS_END##\n");
    }

    //player own cards section
    private void appendPlayerCards(StringBuilder sb, Player player) {
        List<it.polimi.ingsw.model.cards.CharacterCard> chars    = player.getCharacterCards();
        List<it.polimi.ingsw.model.cards.BuildingCard>  buildings = player.getBuildingCards();

        sb.append("\n  ┌── Le tue carte ─────────────────────────────────────────────────┐\n");

        if (chars.isEmpty()) {
            sb.append("  │  [Personaggi] nessuna carta\n");
        } else {
            sb.append("  │  [Personaggi]\n");
            for (it.polimi.ingsw.model.cards.CharacterCard c : chars) {
                sb.append("  │    • ").append(c.toDisplayString()).append("\n");
            }
        }

        if (buildings.isEmpty()) {
            sb.append("  │  [Edifici] nessuna carta\n");
        } else {
            sb.append("  │  [Edifici]\n");
            for (it.polimi.ingsw.model.cards.BuildingCard c : buildings) {
                sb.append("  │    • ").append(c.toDisplayString()).append("\n");
            }
        }

        sb.append("  └──────────────────────────────────────────────────────────────────┘\n");
    }

    //board section
    private void appendBoardDisplay(StringBuilder sb) {

        // ── top row ──────────────────────────────────────────
        sb.append("  ┌── Riga SUPERIORE ────────────────────────────────────────────────┐\n");

        List<TribeCard>   topTribe = game.getBoard().getAvailableUpperTribeCards();
        List<BuildingCard> topBld  = game.getBoard().getAvailableUpperBuildingCards();

        if (topTribe.isEmpty() && topBld.isEmpty()) {
            sb.append("  │  (vuota)\n");
        } else {
            for (TribeCard c : topTribe)   sb.append(String.format("  │  [T] %-52s (CardId: %d)%n", c.toDisplayString(), c.getID()));
            for (BuildingCard c : topBld)  sb.append(String.format("  │  [B] %-52s (CardId: %d)%n", c.toDisplayString(), c.getID()));
        }
        sb.append("  └──────────────────────────────────────────────────────────────────┘\n\n");

        // ── BoardSpaces ───────────────────────────────────────────────────
        sb.append("  ┌── Spazi Offerta ─────────────────────────────────────────────────┐\n");
        sb.append(String.format("  │  %-4s  %-6s  %-6s  %-6s  %-18s%n",
                "Lett.", "↑ Carte", "↓ Carte", "Cibo", "Totem"));

        for (BoardSpace space : game.getBoard().getOfferField()) {
            String occupant = space.isFree() ? "—"
                    : playerNameForTotem(space.getTotem());
            sb.append(String.format("  │  [ %c ]  %-7d  %-7d  %-6d  %-18s%n",
                    space.getLetter(),
                    space.getTopCardsNumber(),
                    space.getBottomCardsNumber(),
                    space.getFoodReward(),
                    occupant));
        }
        sb.append("  └──────────────────────────────────────────────────────────────────┘\n\n");


        // ── bottom row ──────────────────────────────────────────
        sb.append("  ┌── Riga INFERIORE ────────────────────────────────────────────────┐\n");

        List<TribeCard>   botTribe = game.getBoard().getAvailableBottomTribeCards();
        List<BuildingCard> botBld  = game.getBoard().getAvailableBottomBuildingCards();

        if (botTribe.isEmpty() && botBld.isEmpty()) {
            sb.append("  │  (vuota)\n");
        } else {
            for (TribeCard c : botTribe)   sb.append(String.format("  │  [T] %-52s (CardId: %d)%n", c.toDisplayString(), c.getID()));
            for (BuildingCard c : botBld)  sb.append(String.format("  │  [B] %-52s (CardId: %d)%n", c.toDisplayString(), c.getID()));
        }
        sb.append("  └──────────────────────────────────────────────────────────────────┘\n\n");

        // ── free spaces short
        List<String> free = game.getBoard().getFreeBoardSpaces().stream()
                .map(s -> String.valueOf(s.getLetter()))
                .toList();
        sb.append("  Spazi liberi: ").append(String.join(", ", free)).append("\n");



    }

    //players section
    private void appendPlayersSummary(StringBuilder sb) {
        sb.append("\n  ┌── Stato giocatori ──────────────────────────────────────────────┐\n");
        sb.append(String.format("  │  %-18s  %-6s  %-8s  %-10s%n",
                "Nome", "Cibo", "Prestige", "Turno"));
        for (Player p : game.getPlayers()) {
            String turnMark = p.isInTurn() ? "← TUO TURNO" : "";
            sb.append(String.format("  │  %-18s  %-6d  %-8d  %-10s%n",
                    p.getNickname(),
                    p.getFood(),
                    p.getPrestige(),
                    turnMark));
        }
        sb.append("  └──────────────────────────────────────────────────────────────────┘\n");
    }

    //players card section
    private void appendAllPlayersCardsSummary(StringBuilder sb) {
        // Machine-readable block — the GUI looks for PLAYER= / CARD= / END_PLAYER
        sb.append("\n##PLAYER_CARDS_BEGIN##\n");
        for (Player p : game.getPlayers()) {
            sb.append("PLAYER=").append(p.getNickname()).append("\n");
            for (CharacterCard c : p.getCharacterCards())
                sb.append("CARD=").append(c.getID()).append("\n");
            for (BuildingCard c : p.getBuildingCards())
                sb.append("CARD=").append(c.getID()).append("\n");
            sb.append("END_PLAYER\n");
        }
        sb.append("##PLAYER_CARDS_END##\n");

        // Human-readable section — TUI only; the GUI ignores this text
        sb.append("\n┌── Riepilogo carte giocatori ─────────────────────────────\n");
        for (Player p : game.getPlayers()) {
            sb.append("│\n│  ").append(p.getNickname()).append("\n");
            appendCharTypeLine(sb, p, CharacterType.SHAMAN,
                    "Sciamani",     "→  " + p.getStarsFromShamans() + " stelle totali");
            appendCharTypeLine(sb, p, CharacterType.BUILDER,
                    "Costruttori",  "→  sconto build: -" + p.foodDiscountToBuyBuildings() + " cibo");
            appendCharTypeLine(sb, p, CharacterType.COLLECTOR,
                    "Raccoglitori", "→  sconto cibo: -" + p.getCollectorsFoodDiscount());
            appendCharTypeLine(sb, p, CharacterType.INVENTOR,
                    "Inventori",    "→  " + p.getNumInventions() + " invenzioni diverse");
            appendCharTypeLine(sb, p, CharacterType.ARTIST,    "Artisti",    null);
            appendCharTypeLine(sb, p, CharacterType.HUNTER,    "Cacciatori", null);
            List<? extends BuildingCard> bList = p.getBuildingCards();
            if (!bList.isEmpty()) {
                sb.append("│    Costruzioni (").append(bList.size()).append("):\n");
                for (BuildingCard b : bList)
                    sb.append("│      · ").append(buildingLabel(b)).append("\n");
            }
        }
        sb.append("└──────────────────────────────────────────────────────────\n");
    }

    private void appendCharTypeLine(StringBuilder sb, Player p,
                                    CharacterType type, String label, String extra) {
        long count = p.getCharacterCards().stream()
                .filter(c -> c.getCharacterType() == type).count();
        if (count == 0) return;
        sb.append("│    ").append(label).append(": ").append(count);
        if (extra != null) sb.append("  ").append(extra);
        sb.append("\n");
    }

    private String buildingLabel(BuildingCard b) {
        if (b instanceof BuildingEachTurn bet) {
            return switch (bet.getBType()) {
                case BUILDER_DOUBLEPOINTS   -> "×2 punti Costruttori";
                case RITUAL_DOUBLEPOINTS    -> "×2 punti Rituali";
                case RITUAL_THREEEXTRASTARS -> "+3 stelle extra (Rituali)";
                case RITUAL_NOMALUS         -> "Rituali: no malus";
                case EXTRAFOOD_SET          -> "Cibo bonus per set completo";
                case EXTRAFOOD_INVENTORS    -> "Cibo bonus per Inventori doppi";
                case EXTRAFOOD_TURNORDER    -> "Cibo bonus posizione turno";
                case EXTRACARD              -> "Carta extra dalla riga superiore";
            };
        }
        if (b instanceof BuildingEnd be)
            return "Fine partita: +" + be.getPrestigeEndEffect()
                    + " per ogni " + charTypeName(be.getCharacterToConsider());
        if (b instanceof BuildingEvent bev)
            return "Evento " + eventTypeName(bev.getEventToRespond())
                    + ": bonus su " + charTypeName(bev.getCharacterToConsider());
        return b.getClass().getSimpleName();
    }

    private String charTypeName(CharacterType t) {
        if (t == null) return "?";
        return switch (t) {
            case ARTIST     -> "Artisti";
            case BUILDER    -> "Costruttori";
            case COLLECTOR  -> "Raccoglitori";
            case HUNTER     -> "Cacciatori";
            case INVENTOR   -> "Inventori";
            case SHAMAN     -> "Sciamani";
            case SET_OF_CHAR -> "Set completi";
        };
    }

    private String eventTypeName(EventType t) {
        if (t == null) return "?";
        return switch (t) {
            case HUNT       -> "Caccia";
            case PICTURES   -> "Pitture";
            case RITUAL     -> "Rituali";
            case SUSTENANCE -> "Sostentamento";
        };
    }

    private String playerNameForTotem(Totem totem) {
        if (totem == null) return "—";
        return game.getPlayers().stream()
                .filter(p -> p.getTotem() == totem)
                .map(Player::getNickname)
                .findFirst()
                .orElse("?");
    }

    // Build a new animation for event_resolution phase
    private String buildEventAnimationPayload(String details) {
        StringBuilder sb = new StringBuilder();
        sb.append(details).append("\n");

        for (Player p : game.getPlayers()) {
            sb.append("player=").append(p.getNickname())
                    .append(";food=").append(p.getFood())
                    .append(";prestige=").append(p.getPrestige())
                    .append("\n");
        }

        return sb.toString();
    }


    private String buildFinalResults() {
        return game.getPlayers().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()))
                .map(p -> p.getNickname() + ":" + p.getTotalPoints())
                .collect(Collectors.joining(","));
    }


    // ─────────────────────────────────────────────────────────────────────
    //  UTILITY
    // ─────────────────────────────────────────────────────────────────────

    private Player findPlayer(String nickname) {
        return game.getPlayers().stream()
                .filter(p -> p.getNickname().equals(nickname))
                .findFirst().orElse(null);
    }

    private boolean isPlayerTurn(String nickname) {
        Player cur = game.getCurrentPlayer();
        return cur != null && cur.getNickname().equals(nickname);
    }

    private VirtualView viewOf(String nickname) {
        int idx = nicks.indexOf(nickname);
        return idx >= 0 ? clients.get(idx) : null;
    }

    private void safeError(String nickname, String msg) {
        try {
            VirtualView v = viewOf(nickname);
            if (v != null) v.onError(msg);
        } catch (Exception ex) {
            System.err.println("[Controller] safeError: " + ex.getMessage());
        }
    }

    private void safeInvalidAction(String nickname, String msg) {
        try {
            VirtualView v = viewOf(nickname);
            if (v != null) v.onInvalidAction(nickname, msg);
        } catch (Exception ex) {
            System.err.println("[Controller] safeInvalidAction: " + ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface ViewAction {void execute(VirtualView v) throws Exception; }

    private void broadcast(ViewAction action) {
        for (VirtualView v : clients) {
            try { action.execute(v); }
            catch (Exception e) { System.err.println("[Controller] broadcast: " + e.getMessage()); }
        }

        /*
        // === SPECTATOR: spectators receive all public game events ===
        for (VirtualView v : spectators) {
            try { action.execute(v); }
            catch (Exception e) { System.err.println("[Controller] broadcast→spectator: " + e.getMessage()); }
        }
        // === END SPECTATOR ===

         */

    }
}
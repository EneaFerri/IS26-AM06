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

    /** true durante il periodo in cui si aspetta che i giocatori si riconnettano dopo un crash. */
    private boolean recovering = false;

    /** true quando tutti i giocatori reali si sono disconnessi — i bot smettono di agire. */
    private volatile boolean gameAborted = false;

    /** Nickname dei player attualmente sostituiti da un bot (sia in gioco che durante il recovery). */
    private final Set<String> botNicknames = new HashSet<>();

    // === SPECTATOR ===
    private final List<VirtualView> spectators = new CopyOnWriteArrayList<>();
    private final List<String> spectatorNicks = new CopyOnWriteArrayList<>();
    // === END SPECTATOR ===

    private static final TotemColor[] TOTEM_COLORS = TotemColor.values();

    public GameController(Game game) {
        this.game = game;
        game.addObserver(this);
    }

    /** Costruttore per partite ripristinate da disco: recovering=true blocca il salvataggio
     *  e attende che tutti i giocatori si riconnettano prima di riprendere. */
    public GameController(Game game, boolean recovering) {
        this.game = game;
        this.expectedPlayers = game.getNumberOfPlayers();
        this.recovering = recovering;
        game.addObserver(this);
    }

    /**
     * Costruttore per partite ripristinate da disco con bot salvati.
     * I nickname in savedBotNicknames non devono riconnettersi: vengono ricreati
     * automaticamente come Bot non appena tutti i player reali sono tornati.
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


    public synchronized boolean isFinished() {
        return game.getStatus() == GameState.END;
    }

    public synchronized boolean isInProgress() {
        return !isOpen() && !isFinished();
    }

    public synchronized int getCurrentPlayers()  { return game.getNumberOfPlayers(); }

    public int getExpectedPlayers() { return expectedPlayers; }

    // True if a player with that nickname is registered in this lobby.
    public boolean hasPlayer(String nickname) {
        return nicks.contains(nickname);
    }

    public boolean isRecovering() { return recovering; }
    public boolean isAborted()    { return gameAborted; }

    /** True se il Game model (ripristinato da disco) contiene un Player con questo nickname
     *  E quel player non era già un bot al momento del crash (i bot si riconnettono in automatico). */
    public boolean hasPlayerInGame(String nickname) {
        return game.getPlayers().stream().anyMatch(p -> p.getNickname().equals(nickname))
                && !botNicknames.contains(nickname);
    }

    /**
     * Riconnette un client che si sta ricollegando dopo un crash del server.
     * Quando tutti i giocatori attesi si sono riconnessi, riprende la partita
     * inviando onGameStarting + il turno corrente.
     */
    public synchronized void reconnectPlayer(String nickname, VirtualView caller) {
        clients.add(caller);
        nicks.add(nickname);
        try { caller.onLoginAccepted(nickname, expectedPlayers); } catch (Exception ignored) {}

        // I player "reali" da attendere sono quelli che non erano bot al momento del crash.
        int realExpected = expectedPlayers - botNicknames.size();
        System.out.println("[GameController] Reconnected: " + nickname
                + " (" + nicks.size() + "/" + realExpected + " reali, "
                + botNicknames.size() + " bot da respawnare)");

        if (nicks.size() == realExpected) {
            // Tutti i player reali sono tornati → ricrea i bot per i posti mancanti
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

    // === SPECTATOR ===
    //True if a spectator with that nickname is registered in this lobby.
    public boolean hasSpectator(String nickname) {
        return spectatorNicks.contains(nickname);
    }

    /**
     * Adds a spectator: receives all public broadcast events but never onYourTurn.
     * Sends an immediate board snapshot so the spectator sees the current state.
     */
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

    /** Removes a spectator by view reference. */
    public void removeSpectator(String nick) {
        int idx = spectatorNicks.indexOf(nick);
        if (idx >= 0) {
            spectators.remove(idx);
            spectatorNicks.remove(idx);
        }
    }
    // === END SPECTATOR ===


    // ─────────────────────────────────────────────────────────────────────
    //  LOBBY
    // ─────────────────────────────────────────────────────────────────────

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

    @Override public void onPlayerJoined(String nickname) { /* managed in broadcastPlayerJoined */ }

    @Override // errors sended to right clients
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

            // === SPECTATOR: snapshot sent to all spectators at every turn ===
            for (VirtualView spectator : spectators) {
                try { spectator.onTurnSnapshot(nickname, boardSummary); }
                catch (Exception e) {
                    System.err.println("[Controller] onTurnSnapshot → spectator: " + e.getMessage());
                }
            }
            // === END SPECTATOR ===

            // ── 2. Full action panel → active player only ─────────────────
            VirtualView target = viewOf(nickname);
            if (target == null) return;
            String extra = buildExtraInfo(nickname, phase);
            target.onYourTurn(nickname, phase, extra);

        } catch (Exception e) {
            System.err.println("[Controller] onTurnStarted: " + e.getMessage());
        }

        // ── Persistenza: salva dopo ogni cambio turno (non durante il recovery) ──
        if (!recovering) PersistenceManager.getInstance().save(game, botNicknames);
    }

    @Override
    public void onTotemPlaced(String nickname, String boardSpaceId) {
        broadcast(v -> v.onTotemPlaced(nickname, boardSpaceId));
    }

    @Override
    public void onInvalidAction(String nicknameTarget, String errorMessage) {
        try {
            VirtualView t = viewOf(nicknameTarget);
            if (t != null) t.onInvalidAction(nicknameTarget, errorMessage);
        } catch (Exception e) {
            System.err.println("[Controller] onInvalidAction: " + e.getMessage());
        }
    }

    @Override
    public void onCardTaken(String nickname, String cardId) {
        broadcast(v -> v.onCardTaken(nickname, cardId));
    }

    @Override
    public void onPlayerUpdated(String nickname) {
        broadcast(v -> v.onPlayerUpdated(nickname));
    }

    @Override
    public void onTurnOrderUpdated(List<String> ordered) {
        broadcast(v -> v.onTurnOrderUpdated(ordered));
    }

    @Override
    public void onEventResolved(String eventName, String details) {
        String payload = buildEventAnimationPayload(details);
        broadcast(v -> v.onEventResolved(eventName, payload));
    }

    @Override
    public void onBoardUpdated() {
        broadcast(v -> v.onBoardUpdated());
    }

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

        // FA Persistenza: partita finita normalmente → elimina il file di salvataggio
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

        // Se il player è già rappresentato da un Bot, questa notifica è stale:
        // arriva dalla nuova connessione creata dal loop FA4 (che non ha trovato un
        // recovering game e ha aperto una nuova lobby). Ignorare per evitare abort improprio.
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
            return;
        }

        // Replace the disconnected player's VirtualView with a bot.
        Bot bot = new Bot(nickname, this, game);
        clients.set(idx, bot);
        botNicknames.add(nickname);  // traccia il posto come "bot" per la persistenza

        broadcast(v -> v.onPlayerDisconnected(nickname));    // "X si è disconnesso"
        broadcast(v -> v.onPlayerReplacedByBot(nickname));   // "X verrà sostituito da un bot"

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
     * True se il player con quel nickname è attualmente rappresentato da un Bot
     * (non da un client di rete reale). Usato da LobbyManager per preferire
     * le lobby dove il player è ancora un client reale.
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
                    sb.append(String.format("  │  [%d] %s%n", i, topCards.get(i)));
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
                    sb.append(String.format("  │  [%d] %s%n", i, botCards.get(i)));
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

        // inserisco uno snapshot del mazzo corrente la GUI lo usa per tenere sincronizzati era, back della carta e contatore residuo
        appendDeckStatus(sb);

        appendBoardDisplay(sb);
        appendPlayersSummary(sb);
        appendAllPlayersCardsSummary(sb);
        return sb.toString();
    }

    /**
     * Aggiunge un piccolo blocco machine-readable con lo stato del mazzo attivo.
     * Lo tengo separato dal testo umano così la GUI può leggerlo in modo robusto
     * senza fare parsing fragile di label o descrizioni di gioco.
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
                sb.append("  │    • ").append(c).append("\n");
            }
        }

        if (buildings.isEmpty()) {
            sb.append("  │  [Edifici] nessuna carta\n");
        } else {
            sb.append("  │  [Edifici]\n");
            for (it.polimi.ingsw.model.cards.BuildingCard c : buildings) {
                sb.append("  │    • ").append(c).append("\n");
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
            for (TribeCard c : topTribe)   sb.append("  │  [T] ").append(c).append("\n");
            for (BuildingCard c : topBld)  sb.append("  │  [B] ").append(c).append("\n");
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
            for (TribeCard c : botTribe)   sb.append("  │  [T] ").append(c).append("\n");
            for (BuildingCard c : botBld)  sb.append("  │  [B] ").append(c).append("\n");
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
        // Blocco machine-readable — la GUI cerca PLAYER= / CARD= / END_PLAYER
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

        // Sezione human-readable — solo per TUI; la GUI ignora questo testo
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

        // === SPECTATOR: spectators receive all public game events ===
        for (VirtualView v : spectators) {
            try { action.execute(v); }
            catch (Exception e) { System.err.println("[Controller] broadcast→spectator: " + e.getMessage()); }
        }
        // === END SPECTATOR ===

    }
}
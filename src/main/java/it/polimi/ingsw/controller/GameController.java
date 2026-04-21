package it.polimi.ingsw.controller;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.GameObserver;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.cards.TribeCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import it.polimi.ingsw.model.enums.TotemColor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller MVC lato server.
 *
 * - Tiene la mappa nickname → VirtualView
 * - Riceve le azioni dei client, le valida superficialmente e le delega a Game
 * - Implementa GameObserver: traduce le notifiche del model in callback ai client
 * - buildExtraInfo() costruisce il "pannello informativo" inviato al giocatore di turno
 */
public class GameController implements GameObserver {

    private final Game             game;
    private final List<VirtualView> clients   = new ArrayList<>();
    private final List<String>      nicks     = new ArrayList<>(); // indice parallelo a clients
    private int expectedPlayers = -1;

    private static final TotemColor[] TOTEM_COLORS = TotemColor.values();

    public GameController(Game game) {
        this.game = game;
        game.addObserver(this);
    }

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
        TotemColor color = TOTEM_COLORS[game.getNumberOfPlayers() % TOTEM_COLORS.length];
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
    //  AZIONI CLIENT → GAME
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

            game.pickCard(player, available.get(cardIndex));
            // → Game notifica onCardTaken + onPlayerUpdated
            // → se pescate esaurite → returnTotemToTurnOrder + advanceNextPlayer
            // → se era l'ultimo → resolveEvents → nextRound / endGame  (tutto automatico)

        } catch (IllegalStateException e) {
            safeInvalidAction(nickname, e.getMessage());
        } catch (Exception e) {
            System.err.println("[Controller] pickCard: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GameObserver — traduce notifiche model → callback client
    // ─────────────────────────────────────────────────────────────────────

    @Override public void onPlayerJoined(String nickname) { /* gestito in broadcastPlayerJoined */ }
    @Override public void onPlayerError(String message)   { /* gestito inline */ }
    @Override public void onGameStarted()                 { /* gestito in checkAndStartIfReady */ }

    /**
     * Il model ci dice chi deve agire e in che fase.
     * Costruiamo il pannello informativo e lo inviamo SOLO a quel client.
     */
    @Override
    public void onTurnStarted(String nickname, GameState phase) {
        try {
            VirtualView target = viewOf(nickname);
            if (target == null) return;
            String extra = buildExtraInfo(nickname, phase);
            target.onYourTurn(nickname, phase, extra);
        } catch (Exception e) {
            System.err.println("[Controller] onTurnStarted: " + e.getMessage());
        }
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
        broadcast(v -> v.onEventResolved(eventName, details));
    }

    @Override
    public void onBoardUpdated() {
        broadcast(v -> v.onBoardUpdated());
    }

    @Override
    public void onNewEraStarted(Age newEra) {
        broadcast(v -> v.onNewEraStarted(newEra));
    }

    @Override
    public void onGameOver() {
        String results = buildFinalResults();
        broadcast(v -> v.onGameOver(results));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  COSTRUZIONE PANNELLO INFORMATIVO  ← cuore della visualizzazione
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Costruisce il testo multi-riga inviato al giocatore di turno.
     *
     * FASE OFFER_SPACE_CHOOSE
     *   • Riga superiore carte (tribe + building)
     *   • Riga inferiore carte (tribe + building)
     *   • Tabellone BoardSpace con lettera, carte sopra/sotto, cibo, occupazione
     *   • Stato di tutti i giocatori (cibo + prestige)
     *
     * FASE PICKING_CARD
     *   • Pescate rimaste del giocatore
     *   • Lista carte disponibili riga superiore (se ha pescate sopra)
     *   • Lista carte disponibili riga inferiore (se ha pescate sotto)
     *   • Stato di tutti i giocatori
     *   • Cibo del giocatore corrente
     */
    private String buildExtraInfo(String nickname, GameState phase) {
        Player player = findPlayer(nickname);
        if (player == null) return "";

        StringBuilder sb = new StringBuilder();

        if (phase == GameState.OFFER_SPACE_CHOOSE) {
            appendBoardDisplay(sb);
            appendPlayersSummary(sb);

        } else if (phase == GameState.PICKING_CARD) {
            int remTop = game.getRemainingTopPicks(player);
            int remBot = game.getRemainingBottomPicks(player);

            sb.append("  Pescate rimaste → sopra: ").append(remTop)
                    .append("  |  sotto: ").append(remBot).append("\n");
            sb.append("  Il tuo cibo: ").append(player.getFood())
                    .append("  |  Prestige: ").append(player.getPrestige()).append("\n");
            sb.append("\n");

            if (remTop > 0) {
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
        }

        return sb.toString();
    }

    /** Sezione "TABELLONE" con carte e spazi offerta. */
    private void appendBoardDisplay(StringBuilder sb) {

        // ── Riga superiore carte ──────────────────────────────────────────
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

        // ── Riga inferiore carte ──────────────────────────────────────────
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

        // ── Spazi liberi (sintesi rapida) ─────────────────────────────────
        List<String> free = game.getBoard().getFreeBoardSpaces().stream()
                .map(s -> String.valueOf(s.getLetter()))
                .toList();
        sb.append("  Spazi liberi: ").append(String.join(", ", free)).append("\n");
    }

    /** Sezione "STATO GIOCATORI" con cibo e prestige. */
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

    /** Trova il nickname del giocatore che possiede quel totem. */
    private String playerNameForTotem(Totem totem) {
        if (totem == null) return "—";
        return game.getPlayers().stream()
                .filter(p -> p.getTotem() == totem)
                .map(Player::getNickname)
                .findFirst()
                .orElse("?");
    }

    /** Classifica finale ordinata per punti totali. */
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
    private interface ViewAction { void execute(VirtualView v) throws Exception; }

    private void broadcast(ViewAction action) {
        for (VirtualView v : clients) {
            try { action.execute(v); }
            catch (Exception e) { System.err.println("[Controller] broadcast: " + e.getMessage()); }
        }
    }
}
package it.polimi.ingsw.controller;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.database.RankingEntry;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server-side bot that replaces a disconnected player.
 *
 * Implements VirtualView so that GameController treats it exactly like a real
 * network client.
 *
 * Only onYourTurn() and onGameOver() implemented.
 * All other VirtualView methods are not implemented — the bot has no UI.
 */
public class Bot implements VirtualView {

    private static final int ACTION_DELAY_MS = 800;

    private final String           nickname;
    private final GameController   controller;
    private final Game             game;
    private final Random           random = new Random();
    private final ExecutorService  executor;

    Bot(String nickname, GameController controller, Game game) {
        this.nickname   = nickname;
        this.controller = controller;
        this.game       = game;
        // executor created after nickname is assigned so the thread name is correct
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "bot-" + nickname);
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void onYourTurn(String nick, GameState phase, String extraInfo) throws Exception {

        /* Schedule asynchronously: onYourTurn is called from within a synchronized
        block in GameController.onTurnStarted(). Calling controller.placeTotem()
        or controller.pickCard() (both synchronized) from here directly would
        deadlock. The executor thread acquires the lock only after onTurnStarted
         has released it.
         */
        executor.execute(() -> {
            try {
                Thread.sleep(ACTION_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Safety: if the game was aborted (all real players gone) don't act.
            if (controller.isAborted()) return;

            // Safety: only act if it's still our turn (avoid stale invocations).
            Player cur = game.getCurrentPlayer();
            if (cur == null || !cur.getNickname().equals(nickname)) return;

            try {
                // Re-read the current phase instead of using the captured parameter:

                GameState currentPhase = game.getStatus();
                if (currentPhase == GameState.OFFER_SPACE_CHOOSE) {
                    decideAndPlaceTotem();
                } else if (currentPhase == GameState.PICKING_CARD) {
                    decideAndPickCard();
                }
            } catch (Exception e) {
                System.err.println("[Bot " + nickname + "] Action error: " + e.getMessage());
            }
        });
    }

    // Shutdown the executor when the game ends.
    @Override
    public void onGameOver(String results) throws Exception {
        executor.shutdown();
    }


    // ─────────────────────────────────────────────────────────────────────
    //  BOT DECISION LOGIC
    // ─────────────────────────────────────────────────────────────────────

    private void decideAndPlaceTotem() {
        List<BoardSpace> freeSpaces = game.getBoard().getFreeBoardSpaces();
        if (freeSpaces.isEmpty()) {
            System.err.println("[Bot " + nickname + "] No free spaces to place totem.");
            return;
        }
        char letter = freeSpaces.get(random.nextInt(freeSpaces.size())).getLetter();
        System.out.println("[Bot " + nickname + "] Placing totem on space " + letter);
        controller.placeTotem(nickname, letter);
    }

    private void decideAndPickCard() {
        Player player = game.getPlayers().stream()
                .filter(p -> p.getNickname().equals(nickname))
                .findFirst().orElse(null);
        if (player == null) return;

        int remTop = game.getRemainingTopPicks(player);
        int remBot = game.getRemainingBottomPicks(player);

        // ── Build candidate lists for each row ────────────────────────────

        // Top row: tribe cards (excluding events)
        List<Card> topCandidates = new ArrayList<>();
        if (remTop > 0) {
            game.getBoard().getAvailableUpperTribeCards().stream()
                    .filter(c -> !c.isEvent())
                    .forEach(topCandidates::add);
        }

        // Bottom row: tribe cards (excluding events)
        List<Card> botCandidates = new ArrayList<>();
        if (remBot > 0) {
            game.getBoard().getAvailableBottomTribeCards().stream()
                    .filter(c -> !c.isEvent())
                    .forEach(botCandidates::add);
        }

        // ── Choose row and card ───────────────────────────────────────────

        if (topCandidates.isEmpty() && botCandidates.isEmpty()) {
            // No valid pick available — skip remaining picks to unblock the game.
            System.out.println("[Bot " + nickname + "] No valid cards to pick — skipping remaining picks.");
            controller.skipBotPick(nickname);
            return;
        }

        boolean useTop;
        if (topCandidates.isEmpty()) {
            useTop = false;
        } else if (botCandidates.isEmpty()) {
            useTop = true;
        } else {
            useTop = random.nextBoolean();
        }

        List<Card> chosen = useTop ? topCandidates : botCandidates;
        Card card = chosen.get(random.nextInt(chosen.size()));

        // ── Compute the 0-based index that GameController.pickCard() expects ──
        // GameController builds its list as:
        //   fromTop=true  → getAvailableUpperTribeCards()
        //   fromTop=false → getAvailableBottomTribeCards()

        List<Card> fullList = new ArrayList<>();
        if (useTop) {
            fullList.addAll(game.getBoard().getAvailableUpperTribeCards());

        } else {
            fullList.addAll(game.getBoard().getAvailableBottomTribeCards());

        }

        int index = fullList.indexOf(card);
        if (index < 0) {
            System.err.println("[Bot " + nickname + "] Chosen card not found in full list — skipping.");
            controller.skipBotPick(nickname);
            return;
        }

        System.out.println("[Bot " + nickname + "] Picking card index=" + index
                + " fromTop=" + useTop + " card=" + card);
        controller.pickCard(nickname, index, useTop);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  NO-OP VirtualView methods (bot has no UI)
    // ─────────────────────────────────────────────────────────────────────

    @Override public void onLoginAccepted(String nickname, int expectedPlayers)           throws Exception {}
    @Override public void onPlayerJoined(String nickname, int currentCount, int expected) throws Exception {}
    @Override public void onGameStarting(java.util.List<String> playerNicknames)          throws Exception {}
    @Override public void onError(String message)                                         throws Exception {}
    @Override public void onNoLobbyAvailable()                                            throws Exception {}
    @Override public void onLobbyList(java.util.List<LobbyManager.LobbyInfo> lobbies)    throws Exception {}
    @Override public void onTurnSnapshot(String currentPlayerNick, String boardSummary)   throws Exception {}
    @Override public void onTotemPlaced(String nickname, String boardSpaceId)             throws Exception {}
    @Override public void onInvalidAction(String nicknameTarget, String errorMessage)     throws Exception {}
    @Override public void onCardTaken(String nickname, String cardId)                     throws Exception {}
    @Override public void onPlayerUpdated(String nickname)                                throws Exception {}
    @Override public void onTurnOrderUpdated(java.util.List<String> newOrderedNicknames)  throws Exception {}
    @Override public void onEventResolved(String eventName, String resultDetails)         throws Exception {}
    @Override public void onBoardUpdated()                                                throws Exception {}
    @Override public void onNewEraStarted(Age newEra)                                     throws Exception {}
    @Override public void onRankingData(int myRank, int totalEntries,
                                        java.util.List<RankingEntry> fullRanking)         throws Exception {}
    @Override public void onPlayerDisconnected(String nickname)                           throws Exception {}
    @Override public void onPlayerReplacedByBot(String nickname)                          throws Exception {}
    @Override public void onSpectatorJoined(String currentPlayerNick, String boardSummary) throws Exception {}
}

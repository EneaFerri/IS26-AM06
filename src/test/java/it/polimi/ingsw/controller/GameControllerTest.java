package it.polimi.ingsw.controller;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.database.RankingEntry;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.persistence.PersistenceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests all public non-getter GameController methods:
 * - isOpen / isInProgress / isFinished: lobby state checks at various stages.
 * - hasPlayer / isBotPlayer: player-presence and bot-flag queries.
 * - loginFirstPlayer: rejects invalid player counts and duplicate calls; valid call registers player.
 * - login: rejects when no lobby open, on duplicate nickname, and when lobby is full; valid call registers player.
 * - placeTotem: rejects out-of-turn attempts and invalid space letters; valid move produces no error.
 * - pickCard: rejects wrong game state and out-of-turn attempts; out-of-bounds index produces invalidAction.
 * - addSpectator / hasSpectator / removeSpectator: spectator lifecycle.
 */
class GameControllerTest {

    private static final int TEST_ID = 99996;

    private Game game;
    private GameController controller;
    private StubView aliceView;
    private StubView bobView;
    private String firstPlayerNick;   // resolved after startTwoPlayerGame() — order is shuffled

    // ── Stub VirtualView ─────────────────────────────────────────────────────

    private static class StubView implements VirtualView {
        String lastError;
        String lastInvalidAction;
        boolean loginAccepted;
        boolean gameStarting;

        @Override public void onLoginAccepted(String n, int e)           throws Exception { loginAccepted = true; }
        @Override public void onError(String m)                          throws Exception { lastError = m; }
        @Override public void onInvalidAction(String t, String m)        throws Exception { lastInvalidAction = m; }
        @Override public void onGameStarting(List<String> n)             throws Exception { gameStarting = true; }
        @Override public void onPlayerJoined(String n, int c, int e)     throws Exception {}
        @Override public void onNoLobbyAvailable()                       throws Exception {}
        @Override public void onLobbyList(List<LobbyManager.LobbyInfo> l) throws Exception {}
        @Override public void onTurnSnapshot(String n, String b)         throws Exception {}
        @Override public void onYourTurn(String n, GameState p, String e) throws Exception {}
        @Override public void onTotemPlaced(String n, String s)          throws Exception {}
        @Override public void onCardTaken(String n, String c)            throws Exception {}
        @Override public void onPlayerUpdated(String n)                  throws Exception {}
        @Override public void onTurnOrderUpdated(List<String> l)         throws Exception {}
        @Override public void onEventResolved(String e, String d)        throws Exception {}
        @Override public void onBoardUpdated()                           throws Exception {}
        @Override public void onNewEraStarted(Age a)                     throws Exception {}
        @Override public void onGameOver(String r)                       throws Exception {}
        @Override public void onRankingData(int r, int t, List<RankingEntry> l) throws Exception {}
        @Override public void onPlayerDisconnected(String n)             throws Exception {}
        @Override public void onPlayerReplacedByBot(String n)            throws Exception {}
        //@Override public void onSpectatorJoined(String n, String b)      throws Exception {}
    }

    // ── Setup / Teardown ─────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        aliceView  = new StubView();
        bobView    = new StubView();
        game       = new Game(TEST_ID);
        controller = new GameController(game);
    }

    @AfterEach
    void tearDown() {
        PersistenceManager.getInstance().delete(TEST_ID);
    }

    /**
     * Registers Alice + Bob; game auto-starts → OFFER_SPACE_CHOOSE.
     * Turn order is shuffled, so firstPlayerNick is resolved after the call.
     */
    private void startTwoPlayerGame() {
        controller.loginFirstPlayer("Alice", 2, aliceView);
        controller.login("Bob", bobView);
        firstPlayerNick = game.getCurrentPlayer().getNickname();
    }

    /** Returns the second player (the one who is NOT firstPlayerNick). */
    private String secondPlayerNick() {
        return firstPlayerNick.equals("Alice") ? "Bob" : "Alice";
    }

    /** Returns the StubView for the given nickname. */
    private StubView viewOf(String nick) {
        return nick.equals("Alice") ? aliceView : bobView;
    }

    // =========================================================
    // Group A: isOpen / isInProgress / isFinished
    // =========================================================

    @Test
    void isOpen_freshController_returnsTrue() {
        assertTrue(controller.isOpen(), "A brand-new controller should report the lobby as open.");
    }

    @Test
    void isOpen_afterBothPlayersJoined_returnsFalse() {
        startTwoPlayerGame();
        assertFalse(controller.isOpen(), "Lobby should be closed once all expected players have joined.");
    }

    @Test
    void isInProgress_afterBothPlayersJoined_returnsTrue() {
        startTwoPlayerGame();
        assertTrue(controller.isInProgress(), "Game should be in progress after starting.");
    }

    @Test
    void isFinished_freshController_returnsFalse() {
        assertFalse(controller.isFinished(), "A fresh controller should not be in the finished state.");
    }

    // =========================================================
    // Group B: hasPlayer / isBotPlayer
    // =========================================================

    @Test
    void hasPlayer_beforeLogin_returnsFalse() {
        assertFalse(controller.hasPlayer("Alice"), "No player should be registered before any login.");
    }

    @Test
    void hasPlayer_afterLoginFirstPlayer_returnsTrue() {
        controller.loginFirstPlayer("Alice", 2, aliceView);
        assertTrue(controller.hasPlayer("Alice"), "Alice should be registered after loginFirstPlayer.");
    }

    @Test
    void isBotPlayer_realPlayer_returnsFalse() {
        startTwoPlayerGame();
        assertFalse(controller.isBotPlayer("Alice"), "A real player must not be flagged as a bot.");
    }

    // =========================================================
    // Group C: loginFirstPlayer validation
    // =========================================================

    @Test
    void loginFirstPlayer_tooFewPlayers_sendsError() {
        controller.loginFirstPlayer("Alice", 1, aliceView);
        assertNotNull(aliceView.lastError, "Number of players below 2 should produce an error.");
    }

    @Test
    void loginFirstPlayer_tooManyPlayers_sendsError() {
        controller.loginFirstPlayer("Alice", 6, aliceView);
        assertNotNull(aliceView.lastError, "Number of players above 5 should produce an error.");
    }

    @Test
    void loginFirstPlayer_secondCall_sendsLobbyExistsError() {
        controller.loginFirstPlayer("Alice", 2, aliceView);
        StubView bobAttempt = new StubView();
        controller.loginFirstPlayer("Bob", 2, bobAttempt);
        assertNotNull(bobAttempt.lastError, "A second loginFirstPlayer call should be rejected.");
        assertTrue(bobAttempt.lastError.contains("Lobby") || bobAttempt.lastError.contains("lobby"),
                "Error message should state that the lobby already exists.");
    }

    @Test
    void loginFirstPlayer_valid_registersPlayerAndAcceptsLogin() {
        controller.loginFirstPlayer("Alice", 2, aliceView);
        assertTrue(controller.hasPlayer("Alice"), "Alice should be registered after a valid loginFirstPlayer.");
        assertTrue(aliceView.loginAccepted, "Client should receive onLoginAccepted after a valid loginFirstPlayer.");
    }

    // =========================================================
    // Group D: login validation
    // =========================================================

    @Test
    void login_noOpenLobby_sendsError() {
        controller.login("Alice", aliceView);
        assertNotNull(aliceView.lastError, "login without a prior loginFirstPlayer should produce an error.");
    }

    @Test
    void login_duplicateNickname_sendsError() {
        controller.loginFirstPlayer("Alice", 2, aliceView);
        StubView aliceDuplicate = new StubView();
        controller.login("Alice", aliceDuplicate);
        assertNotNull(aliceDuplicate.lastError, "Registering the same nickname twice should produce an error.");
    }

    @Test
    void login_lobbyFull_sendsError() {
        startTwoPlayerGame();
        StubView charlieView = new StubView();
        controller.login("Charlie", charlieView);
        assertNotNull(charlieView.lastError, "Joining a full lobby should produce an error.");
    }

    @Test
    void login_valid_registersPlayer() {
        controller.loginFirstPlayer("Alice", 2, aliceView);
        controller.login("Bob", bobView);
        assertTrue(controller.hasPlayer("Bob"), "Bob should be registered after a valid login.");
    }

    // =========================================================
    // Group E: placeTotem validation
    // =========================================================

    @Test
    void placeTotem_notPlayerTurn_sendsError() {
        startTwoPlayerGame();
        String second = secondPlayerNick();
        controller.placeTotem(second, 'B');
        assertNotNull(viewOf(second).lastError, "Placing a totem out of turn should produce an error.");
    }

    @Test
    void placeTotem_invalidSpaceLetter_sendsInvalidAction() {
        startTwoPlayerGame();
        controller.placeTotem(firstPlayerNick, 'Z');
        assertNotNull(viewOf(firstPlayerNick).lastInvalidAction,
                "An invalid board space letter should produce an invalidAction.");
    }

    @Test
    void placeTotem_validMove_producesNoError() {
        startTwoPlayerGame();
        StubView firstView = viewOf(firstPlayerNick);
        controller.placeTotem(firstPlayerNick, 'B');
        assertNull(firstView.lastError, "A valid totem placement should not produce an error.");
        assertNull(firstView.lastInvalidAction, "A valid totem placement should not produce an invalidAction.");
    }

    // =========================================================
    // Group F: pickCard validation
    // =========================================================

    @Test
    void pickCard_wrongGameState_sendsError() {
        startTwoPlayerGame();
        // Still in OFFER_SPACE_CHOOSE — picking is not allowed yet.
        controller.pickCard(firstPlayerNick, 0, true);
        assertNotNull(viewOf(firstPlayerNick).lastError,
                "Picking a card in OFFER_SPACE_CHOOSE state should produce an error.");
    }

    @Test
    void pickCard_notPlayerTurn_sendsError() {
        startTwoPlayerGame();
        String second = secondPlayerNick();
        controller.pickCard(second, 0, true);
        assertNotNull(viewOf(second).lastError,
                "Picking a card when it is not your turn should produce an error.");
    }

    @Test
    void pickCard_indexOutOfBounds_sendsInvalidAction() {
        startTwoPlayerGame();
        // Advance to PICKING_CARD by having both players place their totems.
        String second = secondPlayerNick();
        controller.placeTotem(firstPlayerNick, 'B');  // first player places
        controller.placeTotem(second, 'C');            // second player places → all placed → PICKING_CARD
        // firstPlayerNick goes first in the pick phase too
        controller.pickCard(firstPlayerNick, 999, true);
        assertNotNull(viewOf(firstPlayerNick).lastInvalidAction,
                "An out-of-bounds card index should produce an invalidAction.");
    }

    // =========================================================
    // Group G: Spectator
    // =========================================================
    /*
    @Test
    void addSpectator_hasSpectator_returnsTrue() {
        startTwoPlayerGame();
        StubView charlieView = new StubView();
        controller.addSpectator("Charlie", charlieView);
        assertTrue(controller.hasSpectator("Charlie"), "Charlie should appear as a spectator after addSpectator.");
    }

    @Test
    void removeSpectator_afterAdd_hasSpectatorReturnsFalse() {
        startTwoPlayerGame();
        StubView charlieView = new StubView();
        controller.addSpectator("Charlie", charlieView);
        controller.removeSpectator("Charlie");
        assertFalse(controller.hasSpectator("Charlie"), "Charlie should no longer be a spectator after removeSpectator.");
    }*/
}

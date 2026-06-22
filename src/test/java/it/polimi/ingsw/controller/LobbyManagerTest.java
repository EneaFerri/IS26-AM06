package it.polimi.ingsw.controller;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.database.RankingEntry;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.persistence.PersistenceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests all public non-getter LobbyManager methods:
 * - createLobby: adds open lobbies to the active list; separate calls create distinct lobbies.
 * - joinSpecificLobby: rejects unknown lobby IDs; valid join registers the second player and starts the game.
 * - handleDisconnect: no-op for unknown players (does not throw).
 * - placeTotem / pickCard: silently ignore actions for players not in any lobby (no throw).
 *
 * Setup: before each test the saves directory is cleaned (game IDs 1–10) so that the LobbyManager
 * constructor finds no persisted snapshots; IDs are cleaned again in tearDown.
 */
class LobbyManagerTest {

    private LobbyManager manager;
    private StubView view;

    // ── Stub VirtualView ─────────────────────────────────────────────────────

    private static class StubView implements VirtualView {
        String lastError;

        @Override public void onLoginAccepted(String n, int e)           throws Exception {}
        @Override public void onError(String m)                          throws Exception { lastError = m; }
        @Override public void onInvalidAction(String t, String m)        throws Exception {}
        @Override public void onGameStarting(List<String> n)             throws Exception {}
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
        PersistenceManager pm = PersistenceManager.getInstance();
        for (int i = 1; i <= 10; i++) pm.delete(i);
        manager = new LobbyManager();
        view    = new StubView();
    }

    @AfterEach
    void tearDown() {
        PersistenceManager pm = PersistenceManager.getInstance();
        for (int i = 1; i <= 10; i++) pm.delete(i);
    }

    // =========================================================
    // Group A: createLobby
    // =========================================================

    @Test
    void createLobby_addsOpenLobbyToActiveList() {
        manager.createLobby("Alice", 2, view);
        List<LobbyManager.LobbyInfo> lobbies = manager.getActiveLobbies();
        assertEquals(1, lobbies.size(), "One open lobby should appear after a single createLobby call.");
        assertFalse(lobbies.get(0).inProgress(), "The newly created lobby should not be in progress yet.");
    }

    @Test
    void createLobby_twoDistinctCalls_listHasTwo() {
        manager.createLobby("Alice", 2, view);
        manager.createLobby("Carol", 3, new StubView());
        assertEquals(2, manager.getActiveLobbies().size(),
                "Two separate createLobby calls should create two distinct lobbies.");
    }

    // =========================================================
    // Group B: joinSpecificLobby
    // =========================================================

    @Test
    void joinSpecificLobby_nonExistentId_sendsError() {
        manager.joinSpecificLobby("Bob", 999, view);
        assertNotNull(view.lastError, "Joining a non-existent lobby ID should produce an error.");
    }

    @Test
    void joinSpecificLobby_valid_startsGame() {
        StubView aliceView = new StubView();
        StubView bobView   = new StubView();
        manager.createLobby("Alice", 2, aliceView);

        int lobbyId = manager.getActiveLobbies().get(0).id();
        manager.joinSpecificLobby("Bob", lobbyId, bobView);

        // Once Bob joins, the game starts and the lobby moves from open to in-progress.
        // getActiveLobbies() lists in-progress lobbies too.
        List<LobbyManager.LobbyInfo> active = manager.getActiveLobbies();
        boolean gameStarted = active.stream().anyMatch(l -> l.id() == lobbyId && l.inProgress());
        assertTrue(gameStarted, "After both players join, the lobby should be in progress.");
    }

    // =========================================================
    // Group C: handleDisconnect / placeTotem / pickCard for unknown players
    // =========================================================

    @Test
    void handleDisconnect_unknownPlayer_doesNotThrow() {
        assertDoesNotThrow(() -> manager.handleDisconnect("Ghost"),
                "Disconnecting an unknown player should not throw.");
    }

    @Test
    void placeTotem_unknownPlayer_doesNotThrow() {
        assertDoesNotThrow(() -> manager.placeTotem("Unknown", 'B'),
                "placeTotem for a player not in any lobby should not throw.");
    }

    @Test
    void pickCard_unknownPlayer_doesNotThrow() {
        assertDoesNotThrow(() -> manager.pickCard("Unknown", 0, true),
                "pickCard for a player not in any lobby should not throw.");
    }
}

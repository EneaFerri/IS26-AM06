package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.database.RankingEntry;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.rmi.server.VirtualViewRmi;
import it.polimi.ingsw.view.ClientModel;
import it.polimi.ingsw.view.ModelObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests RmiClient in two groups:
 *
 * Group A — GameServerProxy delegation: each proxy method (loginFirstPlayer,
 * loginToLobby, requestLobbyList, placeTotem, pickCard, joinAsSpectator,
 * leaveSpectator, ping) is called and the corresponding call on the injected
 * StubVirtualServerRmi is verified.
 *
 * Group B — VirtualViewRmi callbacks: each VirtualViewRmi method is called directly
 * on the RmiClient (simulating a server callback) and the resulting ClientModel
 * delegate call is verified through a StubObserver registered on the ClientModel.
 *
 * The RmiClient is constructed via its injection constructor (no RMI registry needed);
 * UnicastRemoteObject.super() exports it on an ephemeral port, which is unexported
 * in tearDown.
 */
class RmiClientTest {

    private StubVirtualServerRmi stubServer;
    private StubObserver observer;
    private RmiClient client;

    // ── Stubs ─────────────────────────────────────────────────────────────

    private static class StubVirtualServerRmi implements VirtualServerRmi {
        String lastNick;
        int lastNumPlayers;
        int lastLobbyId;
        char lastLetter;
        int lastCardIndex;
        boolean lastFromTop;
        boolean requestLobbyListCalled;

        @Override public void loginFirstPlayer(String n, int num, VirtualViewRmi v) { lastNick = n; lastNumPlayers = num; }
        @Override public void loginToLobby(String n, int id, VirtualViewRmi v) { lastNick = n; lastLobbyId = id; }
        @Override public void requestLobbyList(VirtualViewRmi v) { requestLobbyListCalled = true; }
        @Override public void placeTotem(String n, char l) { lastLetter = l; }
        @Override public void pickCard(String n, int idx, boolean top) { lastCardIndex = idx; lastFromTop = top; }
        //@Override public void joinAsSpectator(String n, int id, VirtualViewRmi v) { lastNick = n; lastLobbyId = id; }
        //@Override public void leaveSpectator(String n, VirtualViewRmi v) { lastNick = n; }
        @Override public void ping() {}
    }

    private static class StubObserver implements ModelObserver {
        String lastLoginNick;
        String lastError;
        String lastDisconnectedNick;
        boolean noLobbyAvailable;
        boolean boardUpdated;
        boolean lobbyListCalled;
        boolean gameStartingCalled;
        boolean gameOverCalled;
        GameState lastPhase;
        Age lastAge;

        @Override public void onLoginAccepted(String n, int e) { lastLoginNick = n; }
        @Override public void onError(String m) { lastError = m; }
        @Override public void onNoLobbyAvailable() { noLobbyAvailable = true; }
        @Override public void onLobbyList(List<LobbyManager.LobbyInfo> l) { lobbyListCalled = true; }
        @Override public void onBoardUpdated() { boardUpdated = true; }
        @Override public void onGameStarting(List<String> p) { gameStartingCalled = true; }
        @Override public void onGameOver(String r) { gameOverCalled = true; }
        @Override public void onYourTurn(String n, GameState p, String e) { lastPhase = p; }
        @Override public void onNewEraStarted(Age a) { lastAge = a; }
        @Override public void onRankingData(int r, int t, List<RankingEntry> l) {}
        @Override public void onPlayerDisconnected(String n) { lastDisconnectedNick = n; }
        @Override public void onPlayerJoined(String n, int c, int e) {}
        @Override public void onTurnSnapshot(String n, String b) {}
        @Override public void onTotemPlaced(String n, String s) {}
        @Override public void onInvalidAction(String t, String m) {}
        @Override public void onCardTaken(String n, String c) {}
        @Override public void onPlayerUpdated(String n) {}
        @Override public void onTurnOrderUpdated(List<String> l) {}
        @Override public void onEventResolved(String e, String d) {}
        @Override public void onPlayerReplacedByBot(String n) {}
        @Override public void onWaitingForServer(String m) {}
        @Override public void onServerReconnected() {}
        //@Override public void onSpectatorJoined(String n, String b) {}
    }

    // ── Setup / Teardown ──────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws RemoteException {
        observer   = new StubObserver();
        ClientModel model = new ClientModel();
        model.registerObserver(observer);
        stubServer = new StubVirtualServerRmi();
        client     = new RmiClient(stubServer, model);
    }

    @AfterEach
    void tearDown() {
        try { UnicastRemoteObject.unexportObject(client, true); } catch (Exception ignored) {}
    }

    // =========================================================
    // Group A: GameServerProxy delegation → StubVirtualServerRmi
    // =========================================================

    @Test
    void loginFirstPlayer_delegatesToServer() throws Exception {
        client.loginFirstPlayer("Alice", 2);
        assertEquals("Alice", stubServer.lastNick);
        assertEquals(2, stubServer.lastNumPlayers);
    }

    @Test
    void loginToLobby_delegatesToServer() throws Exception {
        client.loginToLobby("Bob", 1);
        assertEquals(1, stubServer.lastLobbyId);
    }

    @Test
    void requestLobbyList_delegatesToServer() throws Exception {
        client.requestLobbyList();
        assertTrue(stubServer.requestLobbyListCalled);
    }

    @Test
    void placeTotem_delegatesToServer() throws Exception {
        client.placeTotem("Alice", 'B');
        assertEquals('B', stubServer.lastLetter);
    }

    @Test
    void pickCard_delegatesToServer() throws Exception {
        client.pickCard("Alice", 2, true);
        assertEquals(2, stubServer.lastCardIndex);
        assertTrue(stubServer.lastFromTop);
    }

    /*@Test
    void joinAsSpectator_delegatesToServer() throws Exception {
        client.joinAsSpectator("Charlie", 5);
        assertEquals("Charlie", stubServer.lastNick);
        assertEquals(5, stubServer.lastLobbyId);
    }

    @Test
    void leaveSpectator_delegatesToServer() throws Exception {
        client.leaveSpectator("Charlie");
        assertEquals("Charlie", stubServer.lastNick);
    }*/

    @Test
    void ping_doesNotThrow() {
        assertDoesNotThrow(() -> client.ping(),
                "ping() is a no-op on the client side and should not throw.");
    }

    // =========================================================
    // Group B: VirtualViewRmi callbacks → ClientModel → StubObserver
    // =========================================================

    @Test
    void onLoginAccepted_forwardsNicknameToModel() throws RemoteException {
        client.onLoginAccepted("Alice", 2);
        assertEquals("Alice", observer.lastLoginNick);
    }

    @Test
    void onError_forwardsMessageToModel() throws RemoteException {
        client.onError("something went wrong");
        assertEquals("something went wrong", observer.lastError);
    }

    @Test
    void onGameStarting_callsModelMethod() throws RemoteException {
        client.onGameStarting(List.of("Alice", "Bob"));
        assertTrue(observer.gameStartingCalled);
    }

    @Test
    void onPlayerJoined_doesNotThrow() {
        assertDoesNotThrow(() -> client.onPlayerJoined("Bob", 2, 3));
    }

    @Test
    void onNoLobbyAvailable_callsModelMethod() throws RemoteException {
        client.onNoLobbyAvailable();
        assertTrue(observer.noLobbyAvailable);
    }

    @Test
    void onTurnSnapshot_doesNotThrow() {
        assertDoesNotThrow(() -> client.onTurnSnapshot("Alice", "board"));
    }

    @Test
    void onYourTurn_forwardsPhaseToModel() throws RemoteException {
        client.onYourTurn("Alice", GameState.PICKING_CARD, "x");
        assertEquals(GameState.PICKING_CARD, observer.lastPhase);
    }

    @Test
    void onTotemPlaced_doesNotThrow() {
        assertDoesNotThrow(() -> client.onTotemPlaced("Alice", "B"));
    }

    @Test
    void onCardTaken_doesNotThrow() {
        assertDoesNotThrow(() -> client.onCardTaken("Alice", "c"));
    }

    @Test
    void onBoardUpdated_callsModelMethod() throws RemoteException {
        client.onBoardUpdated();
        assertTrue(observer.boardUpdated);
    }

    @Test
    void onNewEraStarted_forwardsAgeToModel() throws RemoteException {
        client.onNewEraStarted(Age.Era_II);
        assertEquals(Age.Era_II, observer.lastAge);
    }

    @Test
    void onGameOver_callsModelMethod() throws RemoteException {
        client.onGameOver("results");
        assertTrue(observer.gameOverCalled);
    }

    @Test
    void onPlayerDisconnected_forwardsNicknameToModel() throws RemoteException {
        client.onPlayerDisconnected("Bob");
        assertEquals("Bob", observer.lastDisconnectedNick);
    }

    @Test
    void onPlayerReplacedByBot_doesNotThrow() {
        assertDoesNotThrow(() -> client.onPlayerReplacedByBot("Bob"));
    }
}

package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.database.RankingEntry;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.rmi.client.VirtualServerRmi;
import it.polimi.ingsw.persistence.PersistenceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests RmiServer by injecting an anonymous LobbyManager subclass that captures each
 * call and records its arguments. Each VirtualServerRmi method (loginFirstPlayer,
 * loginToLobby, requestLobbyList, placeTotem, pickCard, joinAsSpectator,
 * leaveSpectator, ping) is verified to delegate to the correct LobbyManager method
 * with the correct arguments.
 *
 * The anonymous LobbyManager overrides all delegate methods as no-ops (recording
 * args) to avoid creating real Game objects or accessing the file system. Saves are
 * cleaned before and after each test so the LobbyManager constructor finds no
 * persisted snapshots.
 *
 * The RmiServer is unexported in tearDown to release the ephemeral port.
 */
class RmiServerTest {

    private String lastCreateNick;
    private int lastCreateNumPlayers;
    private boolean requestLobbyListCalled;
    private char lastTotemLetter;
    private int lastCardIndex;
    private boolean lastFromTop;
    //private boolean joinAsSpectatorCalled;
    //private boolean leaveSpectatorCalled;

    private RmiServer server;
    private StubVirtualViewRmi stubView;

    // ── StubVirtualViewRmi ────────────────────────────────────────────────

    private static class StubVirtualViewRmi implements VirtualViewRmi {
        @Override public void onLoginAccepted(String n, int e)                  throws RemoteException {}
        @Override public void onPlayerJoined(String n, int c, int e)            throws RemoteException {}
        @Override public void onGameStarting(List<String> p)                    throws RemoteException {}
        @Override public void onError(String m)                                 throws RemoteException {}
        @Override public void onNoLobbyAvailable()                              throws RemoteException {}
        @Override public void onLobbyList(List<LobbyManager.LobbyInfo> l)      throws RemoteException {}
        @Override public void onTurnSnapshot(String n, String b)               throws RemoteException {}
        @Override public void onYourTurn(String n, GameState p, String e)      throws RemoteException {}
        @Override public void onTotemPlaced(String n, String s)                throws RemoteException {}
        @Override public void onInvalidAction(String t, String m)              throws RemoteException {}
        @Override public void onCardTaken(String n, String c)                  throws RemoteException {}
        @Override public void onPlayerUpdated(String n)                        throws RemoteException {}
        @Override public void onTurnOrderUpdated(List<String> l)               throws RemoteException {}
        @Override public void onEventResolved(String e, String d)              throws RemoteException {}
        @Override public void onBoardUpdated()                                 throws RemoteException {}
        @Override public void onNewEraStarted(Age a)                           throws RemoteException {}
        @Override public void onGameOver(String r)                             throws RemoteException {}
        @Override public void onRankingData(int r, int t, List<RankingEntry> l) throws RemoteException {}
        @Override public void onPlayerDisconnected(String n)                   throws RemoteException {}
        @Override public void onPlayerReplacedByBot(String n)                  throws RemoteException {}
        //@Override public void onSpectatorJoined(String n, String b)            throws RemoteException {}
        @Override public void ping()                                            throws RemoteException {}
    }

    // ── Setup / Teardown ──────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws RemoteException {
        for (int i = 1; i <= 10; i++) PersistenceManager.getInstance().delete(i);

        LobbyManager trackingManager = new LobbyManager() {
            @Override
            public synchronized void createLobby(String nick, int num, VirtualView v) {
                lastCreateNick = nick;
                lastCreateNumPlayers = num;
            }
            @Override
            public synchronized void joinSpecificLobby(String nick, int id, VirtualView v) {}
            @Override
            public synchronized void requestLobbyList(VirtualView v) {
                requestLobbyListCalled = true;
            }
            @Override
            public synchronized void placeTotem(String nick, char l) {
                lastTotemLetter = l;
            }
            @Override
            public synchronized void pickCard(String nick, int idx, boolean top) {
                lastCardIndex = idx;
                lastFromTop   = top;
            }
            /*@Override
            public synchronized void joinAsSpectator(String nick, int id, VirtualView v) {
                joinAsSpectatorCalled = true;
            }
            @Override
            public synchronized void leaveSpectator(String nick, VirtualView v) {
                leaveSpectatorCalled = true;
            }*/
        };

        server   = new RmiServer(trackingManager);
        stubView = new StubVirtualViewRmi();
    }

    @AfterEach
    void tearDown() {
        try { UnicastRemoteObject.unexportObject(server, true); } catch (Exception ignored) {}
        for (int i = 1; i <= 10; i++) PersistenceManager.getInstance().delete(i);
    }

    // =========================================================
    // Group A: VirtualServerRmi → LobbyManager delegation
    // =========================================================

    @Test
    void loginFirstPlayer_delegatesToLobbyManagerCreateLobby() throws RemoteException {
        server.loginFirstPlayer("Alice", 2, stubView);
        assertEquals("Alice", lastCreateNick);
        assertEquals(2, lastCreateNumPlayers);
    }

    @Test
    void loginToLobby_delegatesToLobbyManager() throws RemoteException {
        assertDoesNotThrow(() -> server.loginToLobby("Bob", 1, stubView),
                "loginToLobby should delegate to LobbyManager without throwing.");
    }

    @Test
    void requestLobbyList_delegatesToLobbyManager() throws RemoteException {
        server.requestLobbyList(stubView);
        assertTrue(requestLobbyListCalled);
    }

    @Test
    void placeTotem_delegatesToLobbyManagerWithCorrectLetter() throws RemoteException {
        server.placeTotem("Alice", 'B');
        assertEquals('B', lastTotemLetter);
    }

    @Test
    void pickCard_delegatesToLobbyManagerWithCorrectArgs() throws RemoteException {
        server.pickCard("Alice", 2, true);
        assertEquals(2, lastCardIndex);
        assertTrue(lastFromTop);
    }

    /*@Test
    void joinAsSpectator_delegatesToLobbyManager() throws RemoteException {
        server.joinAsSpectator("Charlie", 1, stubView);
        assertTrue(joinAsSpectatorCalled);
    }

    @Test
    void leaveSpectator_delegatesToLobbyManager() throws RemoteException {
        server.leaveSpectator("Charlie", stubView);
        assertTrue(leaveSpectatorCalled);
    }*/

    @Test
    void ping_doesNotThrow() {
        assertDoesNotThrow(() -> server.ping(),
                "ping() is a no-op server-side and should not throw.");
    }
}

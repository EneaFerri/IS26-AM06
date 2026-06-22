package it.polimi.ingsw.network.socket.client;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.database.RankingEntry;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.utils.PingPongManager;
import it.polimi.ingsw.network.utils.message.MessageType;
import it.polimi.ingsw.network.utils.message.NetworkMessage;
import it.polimi.ingsw.view.ClientModel;
import it.polimi.ingsw.view.ModelObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the private dispatch() method of SocketClient, which translates incoming
 * server-to-client NetworkMessage objects into ClientModel callback invocations.
 *
 * Each test dispatches a NetworkMessage constructed with the exact payload keys
 * used by SocketClientHandler's VirtualView implementations and verifies that the
 * corresponding ModelObserver method was called with the correct arguments.
 *
 * The SocketClient is constructed without calling connect(), so no real socket is
 * involved. The proxy and heartbeat fields are injected via reflection so that
 * PING and PONG dispatch paths can be exercised without a live connection.
 */
class SocketClientDispatchTest {

    private StubObserver observer;
    private SocketClient client;
    private StringWriter proxyBuf;

    // ── Stub ModelObserver ────────────────────────────────────────────────

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
        int lastRank;

        @Override public void onLoginAccepted(String n, int e) { lastLoginNick = n; }
        @Override public void onError(String m) { lastError = m; }
        @Override public void onNoLobbyAvailable() { noLobbyAvailable = true; }
        @Override public void onLobbyList(List<LobbyManager.LobbyInfo> l) { lobbyListCalled = true; }
        @Override public void onBoardUpdated() { boardUpdated = true; }
        @Override public void onGameStarting(List<String> p) { gameStartingCalled = true; }
        @Override public void onGameOver(String r) { gameOverCalled = true; }
        @Override public void onYourTurn(String n, GameState p, String e) { lastPhase = p; }
        @Override public void onNewEraStarted(Age a) { lastAge = a; }
        @Override public void onRankingData(int r, int t, List<RankingEntry> l) { lastRank = r; }
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

    // ── Setup ─────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        observer = new StubObserver();
        ClientModel model = new ClientModel();
        model.registerObserver(observer);
        client   = new SocketClient(model);
        proxyBuf = new StringWriter();

        SocketServerProxy stubProxy = new SocketServerProxy(new PrintWriter(proxyBuf, true));
        Field pf = SocketClient.class.getDeclaredField("proxy");
        pf.setAccessible(true);
        pf.set(client, stubProxy);

        Field hf = SocketClient.class.getDeclaredField("heartbeat");
        hf.setAccessible(true);
        hf.set(client, new PingPongManager());
    }

    private void dispatch(NetworkMessage msg) throws Exception {
        Method m = SocketClient.class.getDeclaredMethod("dispatch", NetworkMessage.class);
        m.setAccessible(true);
        m.invoke(client, msg);
    }

    // =========================================================
    // Group A: login & lobby messages
    // =========================================================

    @Test
    void dispatch_onLoginAccepted_callsModelWithNickname() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_LOGIN_ACCEPTED,
                Map.of("nickname", "Alice", "expectedPlayers", 2)));
        assertEquals("Alice", observer.lastLoginNick);
    }

    @Test
    void dispatch_onPlayerJoined_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> dispatch(new NetworkMessage(MessageType.ON_PLAYER_JOINED,
                Map.of("nickname", "Bob", "currentCount", 2, "expected", 3))));
    }

    @Test
    void dispatch_onGameStarting_callsModel() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_GAME_STARTING,
                Map.of("playerNicknames", List.of("Alice", "Bob"))));
        assertTrue(observer.gameStartingCalled);
    }

    @Test
    void dispatch_onError_forwardsMessage() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_ERROR,
                Map.of("message", "err")));
        assertEquals("err", observer.lastError);
    }

    @Test
    void dispatch_onNoLobbyAvailable_callsModel() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_NO_LOBBY_AVAILABLE));
        assertTrue(observer.noLobbyAvailable);
    }

    @Test
    void dispatch_onLobbyList_callsModel() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_LOBBY_LIST,
                Map.of("lobbies", List.of(
                        Map.of("id", 1, "currentPlayers", 1,
                                "expectedPlayers", 2, "inProgress", false)))));
        assertTrue(observer.lobbyListCalled);
    }

    // =========================================================
    // Group B: turn messages
    // =========================================================

    @Test
    void dispatch_onTurnSnapshot_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> dispatch(new NetworkMessage(MessageType.ON_TURN_SNAPSHOT,
                Map.of("currentPlayerNick", "Alice", "boardSummary", "board"))));
    }

    @Test
    void dispatch_onYourTurn_deserializesPhaseEnum() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_YOUR_TURN,
                Map.of("nickname", "Alice", "phase", "PICKING_CARD", "extraInfo", "x")));
        assertEquals(GameState.PICKING_CARD, observer.lastPhase);
    }

    // =========================================================
    // Group C: phase 1 & 2 messages
    // =========================================================

    @Test
    void dispatch_onTotemPlaced_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> dispatch(new NetworkMessage(MessageType.ON_TOTEM_PLACED,
                Map.of("nickname", "Alice", "boardSpaceId", "B"))));
    }

    @Test
    void dispatch_onInvalidAction_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> dispatch(new NetworkMessage(MessageType.ON_INVALID_ACTION,
                Map.of("nicknameTarget", "Alice", "errorMessage", "e"))));
    }

    @Test
    void dispatch_onCardTaken_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> dispatch(new NetworkMessage(MessageType.ON_CARD_TAKEN,
                Map.of("nickname", "Alice", "cardId", "c"))));
    }

    // =========================================================
    // Group D: end-of-round messages
    // =========================================================

    @Test
    void dispatch_onBoardUpdated_callsModel() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_BOARD_UPDATED));
        assertTrue(observer.boardUpdated);
    }

    @Test
    void dispatch_onNewEraStarted_deserializesAgeEnum() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_NEW_ERA_STARTED,
                Map.of("era", "Era_II")));
        assertEquals(Age.Era_II, observer.lastAge);
    }

    @Test
    void dispatch_onTurnOrderUpdated_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> dispatch(new NetworkMessage(MessageType.ON_TURN_ORDER_UPDATED,
                Map.of("ordered", List.of("Alice", "Bob")))));
    }

    @Test
    void dispatch_onEventResolved_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> dispatch(new NetworkMessage(MessageType.ON_EVENT_RESOLVED,
                Map.of("eventName", "ev", "details", "d"))));
    }

    // =========================================================
    // Group E: end-of-game messages
    // =========================================================

    @Test
    void dispatch_onGameOver_callsModel() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_GAME_OVER,
                Map.of("results", "Alice:100")));
        assertTrue(observer.gameOverCalled);
    }

    @Test
    void dispatch_onRankingData_deserializesCorrectly() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_RANKING_DATA,
                Map.of("myRank", 1,
                        "totalEntries", 5,
                        "ranking", List.of(Map.of(
                                "rank", 1,
                                "nickname", "Alice",
                                "score", 100,
                                "date", "2026-01-01",
                                "numPlayers", 2)))));
        assertEquals(1, observer.lastRank);
    }

    // =========================================================
    // Group F: disconnection messages
    // =========================================================

    @Test
    void dispatch_onPlayerDisconnected_forwardsNickname() throws Exception {
        dispatch(new NetworkMessage(MessageType.ON_PLAYER_DISCONNECTED,
                Map.of("nickname", "Bob")));
        assertEquals("Bob", observer.lastDisconnectedNick);
    }

    @Test
    void dispatch_onPlayerReplacedByBot_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> dispatch(new NetworkMessage(MessageType.ON_PLAYER_REPLACED_BY_BOT,
                Map.of("nickname", "Bob"))));
    }
    /*
    @Test
    void dispatch_onSpectatorJoined_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> dispatch(new NetworkMessage(MessageType.ON_SPECTATOR_JOINED,
                Map.of("currentPlayerNick", "Alice", "boardSummary", "b"))));
    }
    */
    // =========================================================
    // Group G: heartbeat messages
    // =========================================================

    @Test
    void dispatch_ping_sendsPongViaProxy() throws Exception {
        dispatch(new NetworkMessage(MessageType.PING));
        assertTrue(proxyBuf.toString().contains(MessageType.PONG.name()),
                "Dispatching PING should cause the proxy to send PONG.");
    }

    @Test
    void dispatch_pong_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> dispatch(new NetworkMessage(MessageType.PONG)),
                "Dispatching PONG should not throw.");
    }
}

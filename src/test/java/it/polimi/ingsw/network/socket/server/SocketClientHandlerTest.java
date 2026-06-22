package it.polimi.ingsw.network.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.utils.message.MessageType;
import it.polimi.ingsw.network.utils.message.NetworkMessage;
import it.polimi.ingsw.persistence.PersistenceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests SocketClientHandler in two groups:
 *
 * Group A — VirtualView serialization: each VirtualView method is called directly; the
 * resulting JSON is read from the handler's internal sendQueue (accessed via reflection)
 * and verified for the correct MessageType and key payload fields. The run() method is
 * never invoked, so no sender thread or socket I/O is active.
 *
 * Group B — dispatch() deserialization: incoming NetworkMessage objects are fed to the
 * private dispatch() method via reflection. Each test checks that the correct
 * LobbyManager method was invoked (via the real LobbyManager's observable state)
 * or that the correct response type was enqueued.
 *
 * Setup uses a loopback ServerSocket + client Socket pair to provide a valid Socket
 * to the SocketClientHandler constructor.
 */
class SocketClientHandlerTest {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private SocketClientHandler handler;
    private BlockingQueue<String> sendQueue;
    private LobbyManager realManager;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        for (int i = 1; i <= 10; i++) PersistenceManager.getInstance().delete(i);
        serverSocket = new ServerSocket(0);
        clientSocket = new Socket("localhost", serverSocket.getLocalPort());
        Socket serverSide = serverSocket.accept();
        realManager = new LobbyManager();
        handler = new SocketClientHandler(serverSide, realManager);
        Field f = SocketClientHandler.class.getDeclaredField("sendQueue");
        f.setAccessible(true);
        sendQueue = (BlockingQueue<String>) f.get(handler);
    }

    @AfterEach
    void tearDown() throws Exception {
        try { clientSocket.close(); } catch (Exception ignored) {}
        try { serverSocket.close(); } catch (Exception ignored) {}
        for (int i = 1; i <= 10; i++) PersistenceManager.getInstance().delete(i);
    }

    private NetworkMessage lastQueued() throws Exception {
        String json = sendQueue.poll(1, TimeUnit.SECONDS);
        assertNotNull(json, "Handler should have enqueued a message.");
        return mapper.readValue(json, NetworkMessage.class);
    }

    private void dispatchMsg(NetworkMessage msg) throws Exception {
        Method m = SocketClientHandler.class.getDeclaredMethod("dispatch", NetworkMessage.class);
        m.setAccessible(true);
        m.invoke(handler, msg);
    }

    // =========================================================
    // Group A: VirtualView → sendQueue serialization
    // =========================================================

    @Test
    void onLoginAccepted_enqueuedWithCorrectType() throws Exception {
        handler.onLoginAccepted("Alice", 2);
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_LOGIN_ACCEPTED.name(), msg.getType());
        assertEquals("Alice", msg.str("nickname"));
        assertEquals(2, msg.num("expectedPlayers"));
    }

    @Test
    void onError_enqueuedWithMessage() throws Exception {
        handler.onError("bad request");
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_ERROR.name(), msg.getType());
        assertEquals("bad request", msg.str("message"));
    }

    @Test
    void onGameStarting_enqueuedWithPlayerList() throws Exception {
        handler.onGameStarting(List.of("Alice", "Bob"));
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_GAME_STARTING.name(), msg.getType());
        assertFalse(msg.strList("playerNicknames").isEmpty());
    }

    @Test
    void onPlayerJoined_enqueuedCorrectly() throws Exception {
        handler.onPlayerJoined("Bob", 2, 3);
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_PLAYER_JOINED.name(), msg.getType());
        assertEquals("Bob", msg.str("nickname"));
    }

    @Test
    void onNoLobbyAvailable_enqueuedWithEmptyPayload() throws Exception {
        handler.onNoLobbyAvailable();
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_NO_LOBBY_AVAILABLE.name(), msg.getType());
        assertTrue(msg.getPayload().isEmpty());
    }

    @Test
    void onTurnSnapshot_enqueuedWithBoardSummary() throws Exception {
        handler.onTurnSnapshot("Alice", "board summary");
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_TURN_SNAPSHOT.name(), msg.getType());
        assertEquals("board summary", msg.str("boardSummary"));
    }

    @Test
    void onYourTurn_phaseSerializedAsString() throws Exception {
        handler.onYourTurn("Alice", GameState.PICKING_CARD, "extra");
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_YOUR_TURN.name(), msg.getType());
        assertEquals("PICKING_CARD", msg.str("phase"));
    }

    @Test
    void onTotemPlaced_enqueuedCorrectly() throws Exception {
        handler.onTotemPlaced("Alice", "B");
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_TOTEM_PLACED.name(), msg.getType());
        assertEquals("Alice", msg.str("nickname"));
    }

    @Test
    void onCardTaken_enqueuedCorrectly() throws Exception {
        handler.onCardTaken("Alice", "card001");
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_CARD_TAKEN.name(), msg.getType());
        assertEquals("card001", msg.str("cardId"));
    }

    @Test
    void onBoardUpdated_enqueuedWithEmptyPayload() throws Exception {
        handler.onBoardUpdated();
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_BOARD_UPDATED.name(), msg.getType());
        assertTrue(msg.getPayload().isEmpty());
    }

    @Test
    void onNewEraStarted_eraSerializedAsString() throws Exception {
        handler.onNewEraStarted(Age.Era_II);
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_NEW_ERA_STARTED.name(), msg.getType());
        assertEquals("Era_II", msg.str("era"));
    }

    @Test
    void onGameOver_enqueuedWithResults() throws Exception {
        handler.onGameOver("Alice:100,Bob:80");
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_GAME_OVER.name(), msg.getType());
        assertNotNull(msg.str("results"));
    }

    @Test
    void onPlayerDisconnected_enqueuedCorrectly() throws Exception {
        handler.onPlayerDisconnected("Bob");
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_PLAYER_DISCONNECTED.name(), msg.getType());
        assertEquals("Bob", msg.str("nickname"));
    }

    @Test
    void onLobbyList_mapsLobbyInfoToMap() throws Exception {
        handler.onLobbyList(List.of(new LobbyManager.LobbyInfo(1, 1, 2, false)));
        NetworkMessage msg = lastQueued();
        assertEquals(MessageType.ON_LOBBY_LIST.name(), msg.getType());
        assertFalse(msg.mapList("lobbies").isEmpty());
        assertEquals(1, ((Number) msg.mapList("lobbies").get(0).get("id")).intValue());
    }

    // =========================================================
    // Group B: dispatch() — incoming client messages
    // =========================================================

    @Test
    void dispatch_loginFirst_enqueuesLoginAccepted() throws Exception {
        dispatchMsg(new NetworkMessage(MessageType.LOGIN_FIRST,
                Map.of("nickname", "Alice", "numPlayers", 2)));
        NetworkMessage response = lastQueued();
        assertEquals(MessageType.ON_LOGIN_ACCEPTED.name(), response.getType());
    }

    @Test
    void dispatch_requestLobbyList_enqueuesLobbyList() throws Exception {
        dispatchMsg(new NetworkMessage(MessageType.REQUEST_LOBBY_LIST));
        NetworkMessage response = lastQueued();
        assertEquals(MessageType.ON_LOBBY_LIST.name(), response.getType());
    }

    @Test
    void dispatch_pong_doesNotThrowAndDoesNotEnqueue() throws Exception {
        assertDoesNotThrow(() -> dispatchMsg(new NetworkMessage(MessageType.PONG)));
        assertNull(sendQueue.poll(200, TimeUnit.MILLISECONDS),
                "Dispatching PONG should not enqueue any message.");
    }

    @Test
    void dispatch_placeTotemWithoutLogin_doesNotThrow() {
        assertDoesNotThrow(() -> dispatchMsg(new NetworkMessage(MessageType.PLACE_TOTEM,
                        Map.of("letter", "B"))),
                "PLACE_TOTEM before login should not throw.");
    }

    @Test
    void dispatch_loginToLobbyInvalidId_enqueuesError() throws Exception {
        dispatchMsg(new NetworkMessage(MessageType.LOGIN_TO_LOBBY,
                Map.of("nickname", "Bob", "lobbyId", 999)));
        NetworkMessage response = lastQueued();
        assertEquals(MessageType.ON_ERROR.name(), response.getType());
    }

    @Test
    void dispatch_pickCardWithoutLogin_doesNotThrow() {
        assertDoesNotThrow(() -> dispatchMsg(new NetworkMessage(MessageType.PICK_CARD,
                        Map.of("nickname", "X", "cardIndex", 0, "fromTop", true))),
                "PICK_CARD before login should not throw.");
    }
}

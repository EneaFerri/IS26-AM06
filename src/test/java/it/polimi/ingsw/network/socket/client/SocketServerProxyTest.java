package it.polimi.ingsw.network.socket.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.network.utils.message.MessageType;
import it.polimi.ingsw.network.utils.message.NetworkMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests SocketServerProxy by injecting a StringWriter-backed PrintWriter in place of
 * a real socket output stream. Each method call is verified by deserialising the last
 * JSON line written and checking its type and key payload fields.
 *
 * - loginFirstPlayer / loginToLobby / requestLobbyList: lobby management messages.
 * - placeTotem / pickCard: game-action messages with correct payload fields.
 * - joinAsSpectator / leaveSpectator: spectator lifecycle messages.
 * - ping / pong: heartbeat messages with empty payloads.
 * - updateOutput: verifies that swapping the writer redirects subsequent sends.
 */
class SocketServerProxyTest {

    private StringWriter buf;
    private SocketServerProxy proxy;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        buf   = new StringWriter();
        proxy = new SocketServerProxy(new PrintWriter(buf, true));
    }

    private NetworkMessage lastSent() throws Exception {
        String json = buf.toString().trim().lines().reduce((a, b) -> b).orElseThrow();
        return mapper.readValue(json, NetworkMessage.class);
    }

    // =========================================================
    // Group A: login messages
    // =========================================================

    @Test
    void loginFirstPlayer_sendsCorrectTypeAndPayload() throws Exception {
        proxy.loginFirstPlayer("Alice", 2);
        NetworkMessage msg = lastSent();
        assertEquals(MessageType.LOGIN_FIRST.name(), msg.getType());
        assertEquals("Alice", msg.str("nickname"));
        assertEquals(2, msg.num("numPlayers"));
    }

    @Test
    void loginToLobby_sendsCorrectTypeAndPayload() throws Exception {
        proxy.loginToLobby("Bob", 3);
        NetworkMessage msg = lastSent();
        assertEquals(MessageType.LOGIN_TO_LOBBY.name(), msg.getType());
        assertEquals("Bob", msg.str("nickname"));
        assertEquals(3, msg.num("lobbyId"));
    }

    @Test
    void requestLobbyList_sendsRequestMessage() throws Exception {
        proxy.requestLobbyList();
        assertEquals(MessageType.REQUEST_LOBBY_LIST.name(), lastSent().getType());
    }

    // =========================================================
    // Group B: game action messages
    // =========================================================

    @Test
    void placeTotem_sendsLetterInPayload() throws Exception {
        proxy.placeTotem("Alice", 'B');
        NetworkMessage msg = lastSent();
        assertEquals(MessageType.PLACE_TOTEM.name(), msg.getType());
        assertEquals("B", msg.str("letter"));
    }

    @Test
    void pickCard_sendsIndexAndFromTop() throws Exception {
        proxy.pickCard("Alice", 2, true);
        NetworkMessage msg = lastSent();
        assertEquals(MessageType.PICK_CARD.name(), msg.getType());
        assertEquals(2, msg.num("cardIndex"));
        assertTrue(msg.bool("fromTop"));
    }

    // =========================================================
    // Group C: spectator messages
    // =========================================================
    /*
    @Test
    void joinAsSpectator_sendsLobbyId() throws Exception {
        proxy.joinAsSpectator("Charlie", 1);
        NetworkMessage msg = lastSent();
        assertEquals(MessageType.JOIN_AS_SPECTATOR.name(), msg.getType());
        assertEquals(1, msg.num("lobbyId"));
    }

    @Test
    void leaveSpectator_sendsNickname() throws Exception {
        proxy.leaveSpectator("Charlie");
        NetworkMessage msg = lastSent();
        assertEquals(MessageType.LEAVE_SPECTATOR.name(), msg.getType());
        assertEquals("Charlie", msg.str("nickname"));
    }
    */
    // =========================================================
    // Group D: heartbeat messages
    // =========================================================

    @Test
    void ping_sendsEmptyPingMessage() throws Exception {
        proxy.ping();
        NetworkMessage msg = lastSent();
        assertEquals(MessageType.PING.name(), msg.getType());
        assertTrue(msg.getPayload().isEmpty());
    }

    @Test
    void pong_sendsEmptyPongMessage() throws Exception {
        proxy.pong();
        NetworkMessage msg = lastSent();
        assertEquals(MessageType.PONG.name(), msg.getType());
        assertTrue(msg.getPayload().isEmpty());
    }

    // =========================================================
    // Group E: updateOutput
    // =========================================================

    @Test
    void updateOutput_subsequentSendsGoToNewWriter() throws Exception {
        StringWriter buf2 = new StringWriter();
        proxy.updateOutput(new PrintWriter(buf2, true));
        proxy.loginFirstPlayer("Alice", 2);

        assertTrue(buf.toString().isEmpty(),
                "Original buffer should be empty after updateOutput.");
        assertFalse(buf2.toString().isEmpty(),
                "New buffer should contain the JSON after updateOutput.");
    }
}

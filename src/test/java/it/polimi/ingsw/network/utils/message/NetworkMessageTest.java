package it.polimi.ingsw.network.utils.message;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests NetworkMessage's typed payload accessors (str, num, bool, strList, mapList)
 * and its three constructor variants. No network I/O is involved — all tests operate
 * on in-memory objects only.
 */
class NetworkMessageTest {

    // =========================================================
    // Group A: str()
    // =========================================================

    @Test
    void str_existingKey_returnsValue() {
        NetworkMessage msg = new NetworkMessage(MessageType.ON_LOGIN_ACCEPTED,
                Map.of("nickname", "Alice"));
        assertEquals("Alice", msg.str("nickname"));
    }

    @Test
    void str_missingKey_returnsNull() {
        NetworkMessage msg = new NetworkMessage(MessageType.ON_ERROR, Map.of());
        assertNull(msg.str("nickname"));
    }

    // =========================================================
    // Group B: num()
    // =========================================================

    @Test
    void num_existingKey_returnsValue() {
        NetworkMessage msg = new NetworkMessage(MessageType.ON_LOGIN_ACCEPTED,
                Map.of("expectedPlayers", 42));
        assertEquals(42, msg.num("expectedPlayers"));
    }

    @Test
    void num_missingKey_throwsIllegalArgumentException() {
        NetworkMessage msg = new NetworkMessage(MessageType.ON_ERROR, Map.of());
        assertThrows(IllegalArgumentException.class, () -> msg.num("missing"),
                "num() with a missing key should throw IllegalArgumentException.");
    }

    // =========================================================
    // Group C: bool()
    // =========================================================

    @Test
    void bool_existingKeyTrue_returnsTrue() {
        NetworkMessage msg = new NetworkMessage(MessageType.PICK_CARD,
                Map.of("fromTop", true));
        assertTrue(msg.bool("fromTop"));
    }

    @Test
    void bool_missingKey_returnsFalse() {
        NetworkMessage msg = new NetworkMessage(MessageType.PICK_CARD, Map.of());
        assertFalse(msg.bool("missing"),
                "bool() with a missing key should return false.");
    }

    // =========================================================
    // Group D: strList()
    // =========================================================

    @Test
    void strList_existingKey_returnsList() {
        NetworkMessage msg = new NetworkMessage(MessageType.ON_GAME_STARTING,
                Map.of("playerNicknames", List.of("A", "B")));
        assertEquals(List.of("A", "B"), msg.strList("playerNicknames"));
    }

    @Test
    void strList_missingKey_returnsEmptyList() {
        NetworkMessage msg = new NetworkMessage(MessageType.ON_GAME_STARTING, Map.of());
        assertTrue(msg.strList("playerNicknames").isEmpty(),
                "strList() with a missing key should return an empty list.");
    }

    // =========================================================
    // Group E: mapList()
    // =========================================================

    @Test
    void mapList_existingKey_returnsList() {
        NetworkMessage msg = new NetworkMessage(MessageType.ON_LOBBY_LIST,
                Map.of("lobbies", List.of(Map.of("id", 1))));
        List<Map<String, Object>> result = msg.mapList("lobbies");
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("id"));
    }

    @Test
    void mapList_missingKey_returnsEmptyList() {
        NetworkMessage msg = new NetworkMessage(MessageType.ON_LOBBY_LIST, Map.of());
        assertTrue(msg.mapList("lobbies").isEmpty(),
                "mapList() with a missing key should return an empty list.");
    }

    // =========================================================
    // Group F: constructors
    // =========================================================

    @Test
    void noPayloadConstructor_payloadIsEmpty() {
        NetworkMessage msg = new NetworkMessage(MessageType.PING);
        assertTrue(msg.getPayload().isEmpty(),
                "The no-payload constructor should produce an empty payload map.");
    }

    @Test
    void messageTypeConstructor_typeStoredAsEnumName() {
        NetworkMessage msg = new NetworkMessage(MessageType.PING, Map.of());
        assertEquals(MessageType.PING.name(), msg.getType(),
                "The MessageType constructor should store the enum name as the type string.");
    }

    @Test
    void stringTypeConstructor_rawStringPreserved() {
        NetworkMessage msg = new NetworkMessage("CUSTOM", Map.of());
        assertEquals("CUSTOM", msg.getType(),
                "The String constructor should preserve the raw type string.");
    }

    // =========================================================
    // Group G: toString
    // =========================================================

    @Test
    void toString_containsTypeField() {
        NetworkMessage msg = new NetworkMessage(MessageType.PING);
        assertTrue(msg.toString().contains("type"),
                "toString() should include the 'type' field.");
    }
}

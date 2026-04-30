package it.polimi.ingsw.network.utils.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single message unit exchanged over the TCP socket.
 *
 * Wire format: one JSON object per line, terminated by '\n'.
 * Example:
 *   {"type":"PLACE_TOTEM","payload":{"nickname":"Alice","letter":"B"}}
 *
 * Typed accessors (str, num, bool, strList, mapList) avoid raw casts at every call site.
 */
public class NetworkMessage {

    private final String              type;
    private final Map<String, Object> payload;

    // ── Constructors ───────────────────────────────────────────────────────

    /** Jackson deserialisation constructor. */
    @JsonCreator
    public NetworkMessage(
            @JsonProperty("type")    String              type,
            @JsonProperty("payload") Map<String, Object> payload) {
        this.type    = type;
        this.payload = payload != null ? payload : new HashMap<>();
    }

    /** Convenience constructor using the enum. */
    public NetworkMessage(MessageType type, Map<String, Object> payload) {
        this(type.name(), payload != null ? payload : new HashMap<>());
    }

    /** Convenience constructor for messages with no payload (PING, PONG, ON_BOARD_UPDATED…). */
    public NetworkMessage(MessageType type) {
        this(type.name(), new HashMap<>());
    }

    // ── Getters (used by Jackson and by application code) ─────────────────

    public String              getType()    { return type; }
    public Map<String, Object> getPayload() { return payload; }

    // ── Typed payload accessors ────────────────────────────────────────────

    /** Returns payload value as String, or null if absent. */
    public String str(String key) {
        Object v = payload.get(key);
        return v == null ? null : v.toString();
    }

    /** Returns payload value as int. Throws if absent or not a Number. */
    public int num(String key) {
        return ((Number) payload.get(key)).intValue();
    }

    /** Returns payload value as boolean. Absent → false. */
    public boolean bool(String key) {
        return Boolean.TRUE.equals(payload.get(key));
    }

    /** Returns payload value as List<String>. Absent → empty list. */
    @SuppressWarnings("unchecked")
    public List<String> strList(String key) {
        Object v = payload.get(key);
        return v == null ? Collections.emptyList() : (List<String>) v;
    }

    /**
     * Returns payload value as List<Map<String,Object>>.
     * Used for ON_LOBBY_LIST payloads. Absent → empty list.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> mapList(String key) {
        Object v = payload.get(key);
        return v == null ? Collections.emptyList() : (List<Map<String, Object>>) v;
    }

    @Override
    public String toString() {
        return "NetworkMessage{type=" + type + ", payload=" + payload + "}";
    }
}
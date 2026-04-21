package it.polimi.ingsw.network.message;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final String      senderId;   // player nickname, or "SERVER"
    private final Object      payload;    // type-specific DTO, may be null
    private final long        timestamp;  // System.currentTimeMillis() at creation

    /**
     * Full constructor.
     *
     * @param type      message type (never null)
     * @param senderId  originator nickname or "SERVER"
     * @param payload   type-specific data object, or null
     */
    public Message(MessageType type, String senderId, Object payload) {
        if (type == null)     throw new IllegalArgumentException("type must not be null");
        if (senderId == null) throw new IllegalArgumentException("senderId must not be null");
        this.type      = type;
        this.senderId  = senderId;
        this.payload   = payload;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Convenience constructor for messages with no payload
     * (e.g. {@code PING}, {@code END_TURN}).
     */
    public Message(MessageType type, String senderId) {
        this(type, senderId, null);
    }

    // ── Accessors ─────────────────────────────────────────────────────

    public MessageType getType()     { return type; }
    public String      getSenderId() { return senderId; }
    public Object      getPayload()  { return payload; }
    public long        getTimestamp(){ return timestamp; }


    @SuppressWarnings("unchecked")
    public <T> T getPayloadAs(Class<T> clazz) {
        return clazz.isInstance(payload) ? (T) payload : null;
    }

    @Override
    public String toString() {
        return "Message{type=" + type
                + ", from="    + senderId
                + ", payload=" + (payload == null ? "null" : payload.getClass().getSimpleName())
                + ", ts="      + timestamp + "}";
    }
}
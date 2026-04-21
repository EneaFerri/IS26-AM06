package it.polimi.ingsw.network.message;

public enum MessageType {


    // ── Connection & Lobby ────────────────────────────────────────────
    /** Client → Server: wants to join; payload = String nickname. */
    LOGIN_REQUEST,
    /** Server → Client: nickname accepted; payload = LobbyInfo. */
    LOGIN_OK,
    /** Server → Client: nickname rejected; payload = String reason. */
    LOGIN_ERROR,
    /** Client → Server: first player sets lobby size; payload = Integer count. */

    // ── Client → Server actions ───────────────────────────────────────
    /** payload = String cardID. */
    DRAW_CARD,
    /** payload = Position. */
    PLACE_TOTEM,

    // ── Server → All notifications (push) ────────────────────────────
    /** payload = GameState full snapshot. */
    BOARD_UPDATED,
    /** payload = String activePlayerNickname. */
    TURN_CHANGED,
    /** payload = List&lt;PlayerResult&gt;. */
    GAME_OVER,
    /** payload = String nickname of disconnected player. */
    PLAYER_DISCONNECTED,
    /** payload = String reason. Game terminated due to disconnection. */
    GAME_ABORTED,
    /** payload = String description. Sent only to the offending client. */
    ERROR,

    // ── Keep-alive ────────────────────────────────────────────────────
    /** Server → Client every 5 s to detect dead connections. No payload. */
    PING,
    /** Client → Server: response to PING. No payload. */
    PONG

}

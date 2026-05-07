package it.polimi.ingsw.network.utils.message;

/**
 * Exhaustive list of message types exchanged over the TCP socket.
 *
 * Direction conventions:
 *   C→S  client sends to server
 *   S→C  server sends to client
 *   ↔    both directions
 */
public enum MessageType {

    // ── C→S  login ────────────────────────────────────────────────────────
    /** Create a new lobby and register as first player. Payload: nickname, numPlayers */
    LOGIN_FIRST,
    /** Join the first available open lobby.           Payload: nickname              */
    LOGIN,
    /** Join a specific lobby by ID.                   Payload: nickname, lobbyId     */
    LOGIN_TO_LOBBY,
    /** Request the list of active lobbies.            Payload: (none)                */
    REQUEST_LOBBY_LIST,

    // ── C→S  game actions ─────────────────────────────────────────────────
    /** Place totem on an offer space. Payload: nickname, letter */
    PLACE_TOTEM,
    /** Pick a card from a row.        Payload: nickname, cardIndex, fromTop */
    PICK_CARD,

    // ── S→C  lobby & setup ────────────────────────────────────────────────
    /** Payload: nickname, expectedPlayers */
    ON_LOGIN_ACCEPTED,
    /** Payload: nickname, currentCount, expected */
    ON_PLAYER_JOINED,
    /** Payload: playerNicknames (List<String>) */
    ON_GAME_STARTING,
    /** Payload: message */
    ON_ERROR,
    /** Payload: (none) */
    ON_NO_LOBBY_AVAILABLE,
    /** Payload: lobbies (List<Map>) — each map has id, currentPlayers, expectedPlayers */
    ON_LOBBY_LIST,

    // ── S→C  turn ─────────────────────────────────────────────────────────
    /** Broadcast to all waiting players. Payload: currentPlayerNick, boardSummary */
    ON_TURN_SNAPSHOT,
    /** Sent only to the active player.   Payload: nickname, phase, extraInfo */
    ON_YOUR_TURN,

    // ── S→C  phase 1 ──────────────────────────────────────────────────────
    /** Payload: nickname, boardSpaceId */
    ON_TOTEM_PLACED,
    /** Payload: nicknameTarget, errorMessage */
    ON_INVALID_ACTION,

    // ── S→C  phase 2 ──────────────────────────────────────────────────────
    /** Payload: nickname, cardId */
    ON_CARD_TAKEN,
    /** Payload: nickname */
    ON_PLAYER_UPDATED,

    // ── S→C  end of player turn ───────────────────────────────────────────
    /** Payload: ordered (List<String>) */
    ON_TURN_ORDER_UPDATED,

    // ── S→C  end of round / events ────────────────────────────────────────
    /** Payload: eventName, details */
    ON_EVENT_RESOLVED,
    /** Payload: (none) */
    ON_BOARD_UPDATED,
    /** Payload: era (String — Age.name()) */
    ON_NEW_ERA_STARTED,

    // ── S→C  end of game ──────────────────────────────────────────────────
    /** Payload: results (comma-separated "nick:points") */
    ON_GAME_OVER,

    // ── S→C  disconnection ────────────────────────────────────────────────
    /** Broadcast when a player disconnects. Payload: nickname */
    ON_PLAYER_DISCONNECTED,

    // ── ↔  heartbeat ──────────────────────────────────────────────────────
    PING,
    PONG,

    // === SPECTATOR ===
    // ── C→S  spectator ────────────────────────────────────────────────────
    /** Join a game in progress as read-only spectator. Payload: nickname, lobbyId */
    JOIN_AS_SPECTATOR,
    /** Leave spectator mode and return to lobby.       Payload: nickname           */
    LEAVE_SPECTATOR,
    // ── S→C  spectator ────────────────────────────────────────────────────
    /** Initial snapshot sent when spectator joins. Payload: currentPlayerNick, boardSummary */
    ON_SPECTATOR_JOINED
    // === END SPECTATOR ===
}
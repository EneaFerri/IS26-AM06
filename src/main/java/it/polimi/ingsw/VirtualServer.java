package it.polimi.ingsw;

/**
 * Client-to-server action interface, parameterised by the transport-specific VirtualView type.
 *
 * <p>Implemented by {@code RmiServer} (RMI transport) and {@code SocketClientHandler} (TCP transport).
 * The type parameter {@code V} is the concrete remote-callback type for each transport.</p>
 *
 * @param <V> the VirtualView subtype used by the implementing transport
 */
public interface VirtualServer<V extends VirtualView> {

    // --- LOBBY ---

    /**
     * Creates a new lobby and registers the caller as the first player.
     *
     * @param nickname    the creating player's nickname
     * @param numPlayers  total number of players expected (2–5)
     * @param clientView  the caller's callback view
     * @throws Exception if the transport layer encounters an error
     */
    void loginFirstPlayer(String nickname, int numPlayers, V clientView) throws Exception;

    /**
     * Joins a specific lobby by ID (avoids landing in an arbitrary available slot).
     *
     * @param nickname   the joining player's nickname
     * @param lobbyId    1-based ID of the target lobby
     * @param clientView the caller's callback view
     * @throws Exception if the transport layer encounters an error
     */
    void loginToLobby(String nickname, int lobbyId, V clientView)        throws Exception;

    /**
     * Requests the list of all active lobbies (open + in progress).
     *
     * @param clientView the caller's callback view, which will receive {@code onLobbyList}
     * @throws Exception if the transport layer encounters an error
     */
    void requestLobbyList(V clientView)                                   throws Exception;

    // --- PHASE 1: TOTEM PLACEMENT ---

    /**
     * Places the caller's totem on the specified offer space.
     *
     * @param nickname         the acting player's nickname
     * @param boardSpaceLetter letter identifying the target offer space
     * @throws Exception if the transport layer encounters an error
     */
    void placeTotem(String nickname, char boardSpaceLetter)               throws Exception;

    // --- PHASE 2: CARD SELECTION ---

    /**
     * Picks a card from the specified row of the board.
     *
     * @param nickname  the acting player's nickname
     * @param cardIndex 0-based index of the card in the chosen row
     * @param fromTop   true to pick from the top row, false for the bottom row
     * @throws Exception if the transport layer encounters an error
     */
    void pickCard(String nickname, int cardIndex, boolean fromTop)        throws Exception;

    /*
    // === SPECTATOR ===

    /**
     * Joins an in-progress game as a read-only spectator (no game actions allowed).
     *
     * @param nickname   the spectator's nickname
     * @param lobbyId    1-based ID of the target in-progress lobby
     * @param clientView the spectator's callback view
     * @throws Exception if the transport layer encounters an error

    void joinAsSpectator(String nickname, int lobbyId, V clientView)     throws Exception;

    /**
     * Leaves spectator mode and returns to the lobby selection screen.
     *
     * @param nickname   the spectator's nickname
     * @param clientView the spectator's callback view
     * @throws Exception if the transport layer encounters an error

    void leaveSpectator(String nickname, V clientView)                   throws Exception;

    // === END SPECTATOR ===

     */
}
package it.polimi.ingsw.network;

/**
 * Transport-agnostic interface for all client → server actions.
 *
 * CLIView holds a reference to this interface, so it is completely decoupled
 * from the underlying transport (RMI or Socket).
 *
 * Implementations:
 *   - RmiClient          → delegates to the remote VirtualServerRmi stub
 *   - SocketServerProxy  → serialises calls to JSON and writes them over TCP
 */
public interface GameServerProxy {

    // ── Login ──────────────────────────────────────────────────────────────
    /**
     * Creates a new lobby and registers this player as the first member.
     * The underlying transport passes its own callback reference to the server.
     */
    void loginFirstPlayer(String nickname, int numPlayers) throws Exception;

    /** Joins the first available open lobby. */
    void login(String nickname) throws Exception;

    /** Asks the server for the list of open lobbies. */
    void requestLobbyList() throws Exception;

    // ── Phase 1 — totem placement ─────────────────────────────────────────
    void placeTotem(String nickname, char boardSpaceLetter) throws Exception;

    // ── Phase 2 — card selection ──────────────────────────────────────────
    void pickCard(String nickname, int cardIndex, boolean fromTop) throws Exception;
}
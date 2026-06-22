package it.polimi.ingsw.controller;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.persistence.GameSnapshot;
import it.polimi.ingsw.persistence.PersistenceManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages N simultaneous lobbies ({@link GameController} instances).
 *
 * Responsibilities:
 *  - {@link #createLobby}       → called by the first player or by a player who wants a new lobby
 *  - {@link #joinSpecificLobby} → client picks and joins an existing lobby
 *  - {@link #getActiveLobbies}  → snapshot of all active lobbies (open + in progress)
 *  - {@code placeTotem}/{@code pickCard} → find the player's lobby and delegate to it
 *
 * There is exactly one LobbyManager per server instance.
 */

public class LobbyManager {

    private final List<GameController> lobbies = new ArrayList<>();
    private final List<VirtualView> lobbyListSubscribers = new ArrayList<>();

    /** Monotonically increasing counter used to assign unique IDs to new games. */
    private int nextGameId = 1;

    /**
     * Constructor: automatically restores any games that were saved to disk
     * at the previous server shutdown (persistence feature).
     */
    public LobbyManager() {
        List<GameSnapshot> saved = PersistenceManager.getInstance().loadAll();
        for (GameSnapshot snap : saved) {
            try {
                Game restored = PersistenceManager.getInstance().restore(snap);
                // Pass saved bot nicknames: they will be recreated automatically
                // once all real players have reconnected.
                GameController gc = new GameController(restored, true, snap.botNicknames());
                lobbies.add(gc);
                nextGameId = Math.max(nextGameId, snap.gameId() + 1);
                System.out.println("[LobbyManager] Partita ripristinata: game_" + snap.gameId()
                        + " (" + snap.numberOfPlayers() + " giocatori, stato=" + snap.gameState() + ")");
            } catch (Exception e) {
                System.err.println("[LobbyManager] Impossibile ripristinare snapshot game_"
                        + snap.gameId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Creates a new lobby and registers the first player.
     * Finished lobbies are cleaned up first to prevent memory leaks.
     *
     * @param nickname   the creating player's nickname
     * @param numPlayers the total number of players expected in the game
     * @param caller     the creating player's VirtualView
     */
    public synchronized void createLobby(String nickname, int numPlayers, VirtualView caller) {
        cleanFinishedLobbies();
        unregisterLobbyListSubscriber(caller);

        // Persistence: if a recovering game already contains this nickname, reconnect instead.
        if (tryReconnect(nickname, caller)) return;

        GameController lobby = new GameController(new Game(nextGameId++));
        lobbies.add(lobby);
        System.out.println("[LobbyManager] Lobby #" + lobbies.size()
                + " creata da " + nickname + " (" + numPlayers + " giocatori)");
        lobby.loginFirstPlayer(nickname, numPlayers, caller);
        broadcastLobbyListUpdate();
    }

    /**
     * Adds a client to a specific lobby chosen by ID.
     * The CLI shows an ordered list; the GUI uses a button.
     *
     * @param nickname the joining player's nickname
     * @param lobbyId  1-based ID of the target lobby
     * @param caller   the joining player's VirtualView
     */
    public synchronized void joinSpecificLobby(String nickname, int lobbyId, VirtualView caller) {
        // Persistence: if this lobby is in recovery and the player belongs to it, reconnect.
        GameController lobby = findLobbyById(lobbyId);
        if (lobby != null && lobby.isRecovering() && lobby.hasPlayerInGame(nickname)) {
            unregisterLobbyListSubscriber(caller);
            lobby.reconnectPlayer(nickname, caller);
            broadcastLobbyListUpdate();
            return;
        }
        if (lobby == null) {
            try { caller.onError("Lobby #" + lobbyId + " non trovata."); }
            catch (Exception e) { System.err.println("[LobbyManager] joinSpecificLobby error: " + e.getMessage()); }
            return;
        }
        if (!lobby.isOpen()) {
            try { caller.onError("Lobby #" + lobbyId + " non è più aperta."); }
            catch (Exception e) { System.err.println("[LobbyManager] joinSpecificLobby error: " + e.getMessage()); }
            return;
        }
        unregisterLobbyListSubscriber(caller);
        System.out.println("[LobbyManager] " + nickname + " → Lobby #" + lobbyId + " (scelta)");
        lobby.login(nickname, caller);
        broadcastLobbyListUpdate();
    }

    /**
     * Returns a snapshot of all active lobbies.
     * Lobbies with {@code inProgress=false} are still open for new players;
     * lobbies with {@code inProgress=true} are running games that accept spectators.
     *
     * @return list of {@link LobbyInfo} records for open and in-progress lobbies
     */
    public synchronized List<LobbyInfo> getActiveLobbies() {
        cleanFinishedLobbies();
        List<LobbyInfo> result = new ArrayList<>();
        for (int i = 0; i < lobbies.size(); i++) {
            GameController lobby = lobbies.get(i);
            if (lobby.isOpen()) {
                result.add(new LobbyInfo(i + 1, lobby.getCurrentPlayers(), lobby.getExpectedPlayers(), false));
            } else if (lobby.isInProgress()) {
                result.add(new LobbyInfo(i + 1, lobby.getCurrentPlayers(), lobby.getExpectedPlayers(), true));
            }

        }
        return result;
    }

    // ================================================================== //
    //  Delegate actions to the correct GameController                    //
    // ================================================================== //

    /**
     * Delegates a totem-placement action to the lobby that contains the given player.
     *
     * @param nickname the acting player's nickname
     * @param letter   the letter of the target offer space
     */
    public synchronized void placeTotem(String nickname, char letter) {
        GameController lobby = findLobbyOf(nickname);
        if (lobby != null) lobby.placeTotem(nickname, letter);
        else System.err.println("[LobbyManager] placeTotem: lobby not found for " + nickname);
    }

    /**
     * Delegates a card-pick action to the lobby that contains the given player.
     *
     * @param nickname  the acting player's nickname
     * @param cardIndex 0-based index of the card in the chosen row
     * @param fromTop   true for the top row, false for the bottom row
     */
    public synchronized void pickCard(String nickname, int cardIndex, boolean fromTop) {
        GameController lobby = findLobbyOf(nickname);
        if (lobby != null) lobby.pickCard(nickname, cardIndex, fromTop);
        else System.err.println("[LobbyManager] pickCard: lobby not found for " + nickname);
    }

    /**
     * Called by SocketClientHandler (and RmiServer heartbeat) when a client's
     * connection is lost unexpectedly.
     */
    public synchronized void handleDisconnect(String nickname) {
        GameController lobby = findLobbyOf(nickname);
        if (lobby == null) {
            // Might be a spectator — attempt spectator disconnect path.
            //handleSpectatorDisconnect(nickname); SPECTATOR
            return;
        }
        System.out.println("[LobbyManager] Broadcasting disconnect of: " + nickname);
        lobby.onPlayerDisconnected(nickname);
        if (lobby.isAborted()) {
            lobbies.remove(lobby);   // all players disconnected — remove the lobby
            System.out.println("[LobbyManager] Lobby removed (all players disconnected).");
        }
        broadcastLobbyListUpdate();
    }

    /*
    // === SPECTATOR ===

    /**
     * Adds a spectator to the in-progress lobby identified by ID.
     * The spectator immediately receives a board snapshot followed by all subsequent broadcast events.
     *
     * @param nickname the spectator's nickname
     * @param lobbyId  1-based ID of the target in-progress lobby
     * @param caller   the spectator's VirtualView

    public synchronized void joinAsSpectator(String nickname, int lobbyId, VirtualView caller) {
        GameController lobby = findLobbyById(lobbyId);
        if (lobby == null || !lobby.isInProgress()) {
            try { caller.onError("Lobby #" + lobbyId + " non trovata o non in corso."); }
            catch (Exception e) { System.err.println("[LobbyManager] joinAsSpectator error: " + e.getMessage()); }
            return;
        }
        unregisterLobbyListSubscriber(caller);
        System.out.println("[LobbyManager] " + nickname + " → Lobby #" + lobbyId + " (spettatore)");
        lobby.addSpectator(nickname, caller);
    }

    /**
     * Removes a spectator and sends the caller an updated lobby list so they can return to the lobby screen.
     *
     * @param nickname the leaving spectator's nickname
     * @param caller   the spectator's VirtualView

    public synchronized void leaveSpectator(String nickname, VirtualView caller) {
        for (GameController lobby : lobbies) {
            if (lobby.hasSpectator(nickname)) {
                lobby.removeSpectator(nickname);
                System.out.println("[LobbyManager] " + nickname + " left the game as spectator");
                break;
            }
        }
        // Send an updated lobby list so the client can return to the selection screen.
        requestLobbyList(caller);
    }

    private void handleSpectatorDisconnect(String nickname) {
        for (GameController lobby : lobbies) {
            if (lobby.hasSpectator(nickname)) {
                lobby.removeSpectator(nickname);
                System.out.println("[LobbyManager] Spectator disconnected: " + nickname);
                broadcastLobbyListUpdate();
                return;
            }
        }
        System.err.println("[LobbyManager] handleDisconnect: no lobby found for " + nickname);
    }

    // === END SPECTATOR ===
    */


    // ================================================================== //
    //  UTILITY                                                            //
    // ================================================================== //

    /**
     * Looks for a recovering lobby that contains the given nickname as an original player.
     * If found, reconnects the caller to that lobby and returns true.
     *
     * @param nickname the player's nickname
     * @param caller   the player's new VirtualView
     * @return true if the player was reconnected to a recovering game
     */
    private boolean tryReconnect(String nickname, VirtualView caller) {
        for (GameController lobby : lobbies) {
            if (lobby.isRecovering() && lobby.hasPlayerInGame(nickname)) {
                lobby.reconnectPlayer(nickname, caller);
                broadcastLobbyListUpdate();
                return true;
            }
        }
        return false;
    }

    /** Removes finished lobbies to prevent memory leaks. */
    private void cleanFinishedLobbies() {
        lobbies.removeIf(GameController::isFinished);
    }

    /** Returns the first lobby that is still open (not yet full and not started). */
    private GameController findOpenLobby() {
        return lobbies.stream()
                .filter(GameController::isOpen)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the lobby that already contains the player with the given nickname.
     *
     * <p>Prefers the lobby where the player is a real client (not a Bot): when the
     * reconnect loop reconnects a player already replaced by a bot it creates a
     * second lobby with the same nickname. If that second lobby disconnects, we want
     * to find it (where the player is real) rather than the original lobby (where the
     * player is already a bot), to avoid incorrectly aborting the original game.</p>
     *
     * @param nickname the player's nickname to look up
     * @return the matching {@link GameController}, or null if not found
     */
    private GameController findLobbyOf(String nickname) {
        // Prefer a lobby where the player is a real client (not a bot).
        GameController real = lobbies.stream()
                .filter(l -> l.hasPlayer(nickname) && !l.isBotPlayer(nickname))
                .findFirst().orElse(null);
        if (real != null) return real;
        // Fallback: any lobby that contains this nickname (e.g., bot-only).
        return lobbies.stream()
                .filter(l -> l.hasPlayer(nickname))
                .findFirst().orElse(null);
    }

    /**
     * Returns the lobby with the given 1-based ID, or null if out of range.
     *
     * @param lobbyId 1-based lobby identifier
     * @return the matching {@link GameController}, or null if not found
     */
    private GameController findLobbyById(int lobbyId) {
        int idx = lobbyId - 1;
        if (idx < 0 || idx >= lobbies.size()) return null;
        return lobbies.get(idx);
    }

    /**
     * Sends the current lobby list to the caller and registers them as a subscriber
     * for future lobby list updates.
     *
     * @param caller the client requesting the lobby list
     */
    public synchronized void requestLobbyList(VirtualView caller) {
        cleanFinishedLobbies();
        registerLobbyListSubscriber(caller);
        try {
            caller.onLobbyList(getActiveLobbies());
        } catch (Exception e) {
            unregisterLobbyListSubscriber(caller);
            System.err.println("[LobbyManager] requestLobbyList callback: " + e.getMessage());
        }
    }

    private void broadcastLobbyListUpdate() {
        cleanFinishedLobbies();
        if (lobbyListSubscribers.isEmpty()) return;

        List<LobbyInfo> snapshot = getActiveLobbies();
        List<VirtualView> staleSubscribers = new ArrayList<>();
        for (VirtualView subscriber : new ArrayList<>(lobbyListSubscribers)) {
            try {
                subscriber.onLobbyList(snapshot);
            } catch (Exception e) {
                staleSubscribers.add(subscriber);
                System.err.println("[LobbyManager] broadcastLobbyListUpdate: " + e.getMessage());
            }
        }
        lobbyListSubscribers.removeAll(staleSubscribers);
    }

    private void registerLobbyListSubscriber(VirtualView caller) {
        if (!lobbyListSubscribers.contains(caller)) {
            lobbyListSubscribers.add(caller);
        }
    }

    private void unregisterLobbyListSubscriber(VirtualView caller) {
        lobbyListSubscribers.remove(caller);
    }

    // ================================================================== //
    //  DTO — lobby info sent to clients                                  //
    // ================================================================== //

    /**
     * Serialisable snapshot of an active lobby sent to clients.
     * {@code inProgress=true}  → game is running (spectators only)
     * {@code inProgress=false} → lobby is open (accepts new players)
     *
     * @param id             the 1-based lobby identifier
     * @param currentPlayers number of players currently registered
     * @param expectedPlayers total number of players the game is configured for
     * @param inProgress     true if the game has already started
     */
    public record LobbyInfo(int id, int currentPlayers, int expectedPlayers, boolean inProgress)
            implements Serializable {

        /** Returns a human-readable summary of this lobby's status. */
        @Override
        public String toString() {
            String status = inProgress ? "[IN CORSO]" : "[APERTA]  ";
            return "Lobby #" + id + "  " + status
                    + "  [" + currentPlayers + "/" + expectedPlayers + " giocatori]";
        }
    }
}

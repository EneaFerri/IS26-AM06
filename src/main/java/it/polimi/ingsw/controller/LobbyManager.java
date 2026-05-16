package it.polimi.ingsw.controller;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.persistence.GamePersistenceManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manage N lobby (GameController) simultaneously
 *
 * Responsability:
 *  - createLobby()         → if first player or player who decide to create a lobby
 *  - joinLobby()           → find the first free lobby; if there is not -> noptify client ---OLD VERSION
 *  - joinSpecificLobby()   → client choose and join an existing lobby ---NEW VERSION
 *  - getActiveLobbies()    → snapshot of all active lobbies (free and running)
 *
 *  - placeTotem/pickCard   → find player lobby and delegate
 *
 *  Only one lobbymanager for server !
 */

public class LobbyManager {

    private final List<GameController> lobbies = new ArrayList<>();
    private final List<VirtualView> lobbyListSubscribers = new ArrayList<>();
    private final AtomicInteger nextGameId = new AtomicInteger(1);

    /**
     * Loads any games saved on disk from a previous server run and registers them
     * as controllers waiting for players to reconnect.
     */
    public LobbyManager() {
        List<Integer> savedIds = GamePersistenceManager.listSavedGameIds();
        int maxSavedId = savedIds.stream().mapToInt(Integer::intValue).max().orElse(0);
        nextGameId.set(maxSavedId + 1);

        for (int id : savedIds) {
            Game restored = GamePersistenceManager.load(id);
            if (restored == null) continue;
            restored.clearObservers();
            GameController ctrl = new GameController(restored);
            ctrl.initFromRestoredGame();
            lobbies.add(ctrl);
            System.out.println("[LobbyManager] Restored saved game#" + id
                    + " (" + restored.getNumberOfPlayers() + " players)");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PERSISTENCE — reconnect helper
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Checks whether the given nickname belongs to a restored game waiting for
     * reconnection. If so, re-attaches the player and returns true.
     */
    private boolean tryReconnect(String nickname, VirtualView caller) {
        for (GameController lobby : lobbies) {
            if (lobby.isWaitingForReconnection() && lobby.hasPlayer(nickname)) {
                System.out.println("[LobbyManager] " + nickname + " reconnecting to restored game");
                lobby.reconnectPlayer(nickname, caller);
                broadcastLobbyListUpdate();
                return true;
            }
        }
        return false;
    }

    //  LOGIN
    /**
     * crate new lobby and register first player
     * Clean ended lobby first (fix memory leak).
     */
    public synchronized void createLobby(String nickname, int numPlayers, VirtualView caller) {
        if (tryReconnect(nickname, caller)) return;
        cleanFinishedLobbies();
        unregisterLobbyListSubscriber(caller);
        GameController lobby = new GameController(new Game(nextGameId.getAndIncrement()));
        lobbies.add(lobby);
        System.out.println("[LobbyManager] Lobby #" + lobbies.size()
                + " creata da " + nickname + " (" + numPlayers + " giocatori)");
        lobby.loginFirstPlayer(nickname, numPlayers, caller);
        broadcastLobbyListUpdate();
    }

    /** OLD VERSION, NO REAL USAGE
     * client added in the first free lobby
     * if no lobby avaible -> onNoLobbyAvailable().
     */
    public synchronized void joinLobby(String nickname, VirtualView caller) {
        if (tryReconnect(nickname, caller)) return;
        GameController available = findOpenLobby();
        if (available != null) {
            unregisterLobbyListSubscriber(caller);
            int id = lobbies.indexOf(available) + 1;
            System.out.println("[LobbyManager] " + nickname + " → Lobby #" + id);
            available.login(nickname, caller);
            broadcastLobbyListUpdate();
        } else {
            try {
                caller.onNoLobbyAvailable();
            } catch (Exception e) {
                System.err.println("[LobbyManager] onNoLobbyAvailable: " + e.getMessage());
            }
        }
    }

    /** NEW VERSION
     * client added on a specific lobby chosen by ID.
     * CLI --> ordered list /  GUI --> button
     */
    public synchronized void joinSpecificLobby(String nickname, int lobbyId, VirtualView caller) {
        if (tryReconnect(nickname, caller)) return;
        GameController lobby = findLobbyById(lobbyId);
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
     * Snapshot with all the actives lobbies
     *  - inProgress=false → lobby free
     *  - inProgress=true  → running game (free for spectators)
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
    //  Delegate actions to the correct gamecontrol              //
    // ================================================================== //

    public synchronized void placeTotem(String nickname, char letter) {
        GameController lobby = findLobbyOf(nickname);
        if (lobby != null) lobby.placeTotem(nickname, letter);
        else System.err.println("[LobbyManager] placeTotem: lobby non trovata per " + nickname);
    }

    public synchronized void pickCard(String nickname, int cardIndex, boolean fromTop) {
        GameController lobby = findLobbyOf(nickname);
        if (lobby != null) lobby.pickCard(nickname, cardIndex, fromTop);
        else System.err.println("[LobbyManager] pickCard: lobby non trovata per " + nickname);
    }

    /**
     * Called by SocketClientHandler (and RmiServer heartbeat) when a client's
     * connection is lost unexpectedly.
     */
    public synchronized void handleDisconnect(String nickname) {
        GameController lobby = findLobbyOf(nickname);
        if (lobby == null) {
            // Potrebbe essere uno spettatore
            handleSpectatorDisconnect(nickname);
            return;
        }
        System.out.println("[LobbyManager] Broadcasting disconnect of: " + nickname);
        lobby.onPlayerDisconnected(nickname);
        broadcastLobbyListUpdate();
    }

    // === SPECTATOR ===

    /**
     * Add a spectator to the lobby chosen
     *Spectators receive snapshots of the game.
     */
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
     *spectator leave and return to the lobby.
     */
    public synchronized void leaveSpectator(String nickname, VirtualView caller) {
        for (GameController lobby : lobbies) {
            if (lobby.hasSpectator(nickname)) {
                lobby.removeSpectator(nickname);
                System.out.println("[LobbyManager] " + nickname + " ha lasciato la partita come spettatore");
                break;
            }
        }
       //lobby sended to the client --> so can chose another game
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


    // ================================================================== //
    //  UTILITY                                                            //
    // ================================================================== //


    private void cleanFinishedLobbies() {
        lobbies.removeIf(GameController::isFinished);
    }

    private GameController findOpenLobby() {
        return lobbies.stream()
                .filter(GameController::isOpen)
                .findFirst()
                .orElse(null);
    }


    private GameController findLobbyOf(String nickname) {
        return lobbies.stream()
                .filter(l -> l.hasPlayer(nickname))
                .findFirst()
                .orElse(null);
    }


    private GameController findLobbyById(int lobbyId) {
        int idx = lobbyId - 1;
        if (idx < 0 || idx >= lobbies.size()) return null;
        return lobbies.get(idx);
    }

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
    //  DTO — info lobby inviata ai client                                //
    // ================================================================== //

    /**
     * Snapshot of active lobby
     * inProgress=true  → started game (for spectators)
     * inProgress=false → free lobby
     */
    public record LobbyInfo(int id, int currentPlayers, int expectedPlayers, boolean inProgress)
            implements Serializable {

        @Override
        public String toString() {
            String status = inProgress ? "[IN CORSO]" : "[APERTA]  ";
            return "Lobby #" + id + "  " + status
                    + "  [" + currentPlayers + "/" + expectedPlayers + " giocatori]";
        }
    }
}

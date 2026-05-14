package it.polimi.ingsw.controller;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.model.Game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestisce N lobby (GameController) in parallelo.
 *
 * Responsabilità:
 *  - createLobby()         → crea una nuova lobby (client è il primo)
 *  - joinLobby()           → trova la prima lobby aperta; se non c'è → notifica il client
 *  - joinSpecificLobby()   → entra in una lobby specifica per ID
 *  - getActiveLobbies()    → snapshot di tutte le lobby attive (aperte + in corso)
 *  - placeTotem/pickCard   → trova la lobby del giocatore e delega
 *
 * È posseduto da RmiServer (uno solo per server).
 */
public class LobbyManager {

    private final List<GameController> lobbies = new ArrayList<>();
    private final List<VirtualView> lobbyListSubscribers = new ArrayList<>();

    //  LOGIN
    /**
     * Crea una lobby nuova e registra il primo giocatore.
     * Pulisce le lobby terminate prima di creare (fix memory leak).
     */
    public synchronized void createLobby(String nickname, int numPlayers, VirtualView caller) {
        cleanFinishedLobbies();
        unregisterLobbyListSubscriber(caller);
        GameController lobby = new GameController(new Game(1));
        lobbies.add(lobby);
        System.out.println("[LobbyManager] Lobby #" + lobbies.size()
                + " creata da " + nickname + " (" + numPlayers + " giocatori)");
        lobby.loginFirstPlayer(nickname, numPlayers, caller);
        broadcastLobbyListUpdate();
    }

    /**
     * Unisce il client alla prima lobby aperta.
     * Se non ce ne sono, notifica il client tramite onNoLobbyAvailable().
     */
    public synchronized void joinLobby(String nickname, VirtualView caller) {
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

    /**
     * Unisce il client a una lobby specifica per ID.
     * Usato da CLI (lista numerata) e GUI (bottone con ID).
     */
    public synchronized void joinSpecificLobby(String nickname, int lobbyId, VirtualView caller) {
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
     * Restituisce snapshot di tutte le lobby attive:
     *  - inProgress=false → lobby aperta (accetta nuovi giocatori)
     *  - inProgress=true  → partita in corso (accessibile come spettatore)
     * Le lobby terminate vengono ripulite.
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
            // lobby terminate escluse
        }
        return result;
    }

    // ================================================================== //
    //  AZIONI DI GIOCO — instradamento alla lobby giusta                 //
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
     * Aggiunge uno spettatore alla lobby in corso specificata per ID.
     * Lo spettatore riceve subito uno snapshot e poi tutti gli eventi broadcast.
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
     * Rimuove lo spettatore e invia lista lobby aggiornata al caller (per tornare alla lobby).
     */
    public synchronized void leaveSpectator(String nickname, VirtualView caller) {
        for (GameController lobby : lobbies) {
            if (lobby.hasSpectator(nickname)) {
                lobby.removeSpectator(nickname);
                System.out.println("[LobbyManager] " + nickname + " ha lasciato la partita come spettatore");
                break;
            }
        }
        // Rimanda la lista lobby aggiornata così il client può tornare alla schermata di selezione
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

    /** Rimuove le lobby terminate (fix memory leak). */
    private void cleanFinishedLobbies() {
        lobbies.removeIf(GameController::isFinished);
    }

    /** Prima lobby con posti liberi e partita non ancora iniziata. */
    private GameController findOpenLobby() {
        return lobbies.stream()
                .filter(GameController::isOpen)
                .findFirst()
                .orElse(null);
    }

    /** Lobby che contiene già il giocatore con quel nickname. */
    private GameController findLobbyOf(String nickname) {
        return lobbies.stream()
                .filter(l -> l.hasPlayer(nickname))
                .findFirst()
                .orElse(null);
    }

    /** Lobby con un ID specifico (1-based). */
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
     * Snapshot serializzabile di una lobby attiva.
     * inProgress=true  → partita in corso (solo spettatori)
     * inProgress=false → lobby aperta (accetta giocatori)
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

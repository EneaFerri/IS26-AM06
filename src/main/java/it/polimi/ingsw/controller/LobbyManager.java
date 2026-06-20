package it.polimi.ingsw.controller;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.persistence.GameSnapshot;
import it.polimi.ingsw.persistence.PersistenceManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Manage N lobby (GameController) simultaneously
 *
 * Responsability:
 *  - createLobby()         → if first player or player who decide to create a lobby
 *  - joinSpecificLobby()   → client choose and join an existing lobby
 *  - getActiveLobbies()    → snapshot of all active lobbies (free and running)
 *
 *  - placeTotem/pickCard   → find player lobby and delegate
 *
 *  Only one lobbymanager for server !
 */

public class LobbyManager {

    private final List<GameController> lobbies = new ArrayList<>();
    private final List<VirtualView> lobbyListSubscribers = new ArrayList<>();

    /** Contatore monotonico per assegnare ID univoci alle nuove partite. */
    private int nextGameId = 1;

    /**
     * Costruttore: al riavvio del server ripristina automaticamente le partite
     * salvate su disco (FA Persistenza).
     */
    public LobbyManager() {
        List<GameSnapshot> saved = PersistenceManager.getInstance().loadAll();
        for (GameSnapshot snap : saved) {
            try {
                Game restored = PersistenceManager.getInstance().restore(snap);
                // Passa i nickname dei bot salvati: verranno ricreati automaticamente
                // quando tutti i player reali si riconnetteranno.
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

    //  LOGIN
    /**
     * crate new lobby and register first player
     * Clean ended lobby first (fix memory leak).
     */
    public synchronized void createLobby(String nickname, int numPlayers, VirtualView caller) {
        cleanFinishedLobbies();
        unregisterLobbyListSubscriber(caller);

        // FA Persistenza: se esiste una partita in recovery con questo nickname, riconnettiti
        if (tryReconnect(nickname, caller)) return;

        GameController lobby = new GameController(new Game(nextGameId++));
        lobbies.add(lobby);
        System.out.println("[LobbyManager] Lobby #" + lobbies.size()
                + " creata da " + nickname + " (" + numPlayers + " giocatori)");
        lobby.loginFirstPlayer(nickname, numPlayers, caller);
        broadcastLobbyListUpdate();
    }

    /**
     * client added on a specific lobby chosen by ID.
     * CLI --> ordered list /  GUI --> button
     */
    public synchronized void joinSpecificLobby(String nickname, int lobbyId, VirtualView caller) {
        // FA Persistenza: se questa lobby è in recovery e il player appartiene ad essa
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
        if (lobby.isAborted()) {
            lobbies.remove(lobby);   // tutti i player si sono disconnessi → rimuovi la lobby
            System.out.println("[LobbyManager] Lobby rimossa (tutti i giocatori disconnessi).");
        }
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

    /**
     * FA Persistenza: cerca una lobby in stato recovering che contenga il nickname
     * come giocatore originale. Se trovata, vi riconnette il caller e ritorna true.
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

    /**
     * Lobby che contiene già il giocatore con quel nickname.
     *
     * Preferisce la lobby in cui il player è un client reale (non un Bot):
     * quando il loop FA4 riconnette un player già sostituito da un bot, crea una
     * seconda lobby (GameN) con quel nickname. Se GameN si disconnette, vogliamo
     * trovare GameN (dove il player è reale) e non la lobby originale (dove è già
     * un bot), altrimenti quella verrebbe abbortita impropriamente.
     */
    private GameController findLobbyOf(String nickname) {
        // Prima cerca una lobby dove il player è un client reale (non bot)
        GameController real = lobbies.stream()
                .filter(l -> l.hasPlayer(nickname) && !l.isBotPlayer(nickname))
                .findFirst().orElse(null);
        if (real != null) return real;
        // Fallback: qualsiasi lobby che contiene quel nickname (es. solo bot)
        return lobbies.stream()
                .filter(l -> l.hasPlayer(nickname))
                .findFirst().orElse(null);
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

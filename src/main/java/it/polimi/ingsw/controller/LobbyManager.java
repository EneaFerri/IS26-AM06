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
 *  - joinOrCreateLobby()   → trova la prima lobby aperta; se non c'è → notifica il client
 *  - getOpenLobbies()      → snapshot delle lobby con posti disponibili
 *  - placeTotem/pickCard   → trova la lobby del giocatore e delega
 *
 * È posseduto da RmiServer (uno solo per server).
 */
public class LobbyManager {

    private final List<GameController> lobbies = new ArrayList<>();

    // ================================================================== //
    //  LOGIN                                                              //
    // ================================================================== //

    /**
     * Crea una lobby nuova e registra il primo giocatore.
     * Chiamato quando il client risponde "sì" a "Vuoi creare una nuova lobby?".
     */
    public synchronized void createLobby(String nickname, int numPlayers, VirtualView caller) {
        GameController lobby = new GameController(new Game(1));
        lobbies.add(lobby);
        System.out.println("[LobbyManager] Lobby #" + lobbies.size()
                + " creata da " + nickname + " (" + numPlayers + " giocatori)");
        lobby.loginFirstPlayer(nickname, numPlayers, caller);
    }

    /**
     * Unisce il client alla prima lobby aperta.
     * Se non ce ne sono, notifica il client tramite onNoLobbyAvailable().
     * Chiamato quando il client risponde "no" a "Vuoi creare una nuova lobby?".
     */
    public synchronized void joinLobby(String nickname, VirtualView caller) {
        GameController available = findOpenLobby();
        if (available != null) {
            int id = lobbies.indexOf(available) + 1;
            System.out.println("[LobbyManager] " + nickname + " → Lobby #" + id);
            available.login(nickname, caller);
        } else {
            try {
                caller.onNoLobbyAvailable();
            } catch (Exception e) {
                System.err.println("[LobbyManager] onNoLobbyAvailable: " + e.getMessage());
            }
        }
    }

    /**
     * Restituisce snapshot delle lobby ancora aperte (posti disponibili, partita non iniziata).
     * Usato da requestLobbyList() per mostrare le opzioni al client.
     */
    public synchronized List<LobbyInfo> getOpenLobbies() {
        List<LobbyInfo> result = new ArrayList<>();
        for (int i = 0; i < lobbies.size(); i++) {
            GameController lobby = lobbies.get(i);
            if (lobby.isOpen()) {
                result.add(new LobbyInfo(
                        i + 1,
                        lobby.getCurrentPlayers(),
                        lobby.getExpectedPlayers()
                ));
            }
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

    // ================================================================== //
    //  UTILITY                                                            //
    // ================================================================== //

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

    // ================================================================== //
    //  DTO — info lobby inviata ai client                                //
    // ================================================================== //

    /**
     * Snapshot serializzabile di una lobby aperta.
     * Implementa Serializable perché viaggia via RMI come parametro di onLobbyList().
     */
    public record LobbyInfo(int id, int currentPlayers, int expectedPlayers)
            implements Serializable {

        @Override
        public String toString() {
            return "Lobby #" + id
                    + "  [" + currentPlayers + "/" + expectedPlayers + " giocatori]";
        }
    }
}
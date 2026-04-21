package it.polimi.ingsw.network_rmi_bozza;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia RMI esposta dal client.
 * Il server chiama questi metodi per inviare aggiornamenti e notifiche (callback).
 */
public interface IGameClient extends Remote {

    /**
     * Conferma di login OK: invia al client il suo nickname confermato
     * e il numero di giocatori attesi per avviare la partita.
     */
    void onLoginAccepted(String nickname, int expectedPlayers) throws RemoteException;

    /**
     * Notifica che un nuovo giocatore si è unito alla lobby.
     * @param nickname     nickname del giocatore entrato
     * @param currentCount numero attuale di giocatori in lobby
     * @param expected     numero di giocatori necessari per iniziare
     */
    void onPlayerJoined(String nickname, int currentCount, int expected) throws RemoteException;

    /**
     * Notifica che la partita sta per iniziare.
     * Contiene i nickname di tutti i giocatori in ordine di ingresso.
     */
    void onGameStarting(java.util.List<String> playerNicknames) throws RemoteException;

    /**
     * Notifica di errore generico (es. nickname duplicato, lobby piena).
     */
    void onError(String message) throws RemoteException;
}

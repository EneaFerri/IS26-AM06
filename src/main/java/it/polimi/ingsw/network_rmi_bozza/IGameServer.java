package it.polimi.ingsw.network_rmi_bozza;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia RMI esposta dal server.
 * Il client chiama questi metodi come se fossero locali.
 */
public interface IGameServer extends Remote {

    /**
     * Primo client che si connette: sceglie quanti giocatori parteciperanno.
     * @param nickname    nickname del client
     * @param numPlayers  numero di giocatori desiderati (2-5)
     * @param clientStub  stub RMI del client, usato per le callback
     */
    void loginFirstPlayer(String nickname, int numPlayers, IGameClient clientStub)
            throws RemoteException;

    /**
     * Client successivi: si uniscono alla lobby già creata.
     * @param nickname   nickname del client
     * @param clientStub stub RMI del client, usato per le callback
     */
    void login(String nickname, IGameClient clientStub)
            throws RemoteException;
}

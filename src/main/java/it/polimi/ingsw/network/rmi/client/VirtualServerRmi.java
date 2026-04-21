package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.VirtualServer;
import it.polimi.ingsw.network.rmi.server.VirtualViewRmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Specializzazione RMI di VirtualServer.
 * Aggiunge extends Remote e throws RemoteException a tutti i metodi,
 * più il metodo connect() per registrare il client presso il server.
 *
 * Corrisponde a VirtualServerRmi nell'esempio dei prof.
 */
public interface VirtualServerRmi extends Remote, VirtualServer {

    /**
     * Registra il client presso il server (passa lo stub RMI per le callback).
     * Chiamato subito dopo la connessione al registry.
     */
    void connect(VirtualViewRmi client) throws RemoteException;

    @Override
    void loginFirstPlayer(String nickname, int numPlayers) throws RemoteException;

    @Override
    void login(String nickname) throws RemoteException;
}

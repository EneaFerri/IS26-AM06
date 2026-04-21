package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.VirtualView;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Specializzazione RMI di VirtualView.
 * Aggiunge extends Remote e throws RemoteException a tutti i metodi.
 *
 * È l'interfaccia che il server usa per fare callback ai client via RMI.
 * Corrisponde a VirtualViewRmi nell'esempio dei prof.
 */
public interface VirtualViewRmi extends Remote, VirtualView {

    @Override
    void onLoginAccepted(String nickname, int expectedPlayers) throws RemoteException;

    @Override
    void onPlayerJoined(String nickname, int currentCount, int expected) throws RemoteException;

    @Override
    void onGameStarting(List<String> playerNicknames) throws RemoteException;

    @Override
    void onError(String message) throws RemoteException;
}

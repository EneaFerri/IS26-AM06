package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.network.rmi.server.VirtualViewRmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface VirtualServerRmi extends Remote {

    void connect(VirtualViewRmi client) throws RemoteException;

    void placeTotem(String player, int pos) throws RemoteException;

    void pickCard(String player, int index) throws RemoteException;

    void connectClient(VirtualViewRmi client);

    void connect(String nickname);

    void startGame();

    void placeTotem(String nickname, char boardSpace);
}
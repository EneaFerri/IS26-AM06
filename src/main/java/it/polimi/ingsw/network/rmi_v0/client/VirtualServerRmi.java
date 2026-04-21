package it.polimi.ingsw.network.rmi_v0.client;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.rmi_v0.server.VirtualViewRmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface VirtualServerRmi extends Remote {

    void connect(VirtualViewRmi client) throws RemoteException;

    void placeTotem(String player, int pos) throws RemoteException;

    void pickCard(String player, int index) throws RemoteException;

    void connectClient(VirtualViewRmi client);

    void connect(String nickname, TotemColor col);

    void startGame();

    void placeTotem(String nickname, char boardSpace);
}
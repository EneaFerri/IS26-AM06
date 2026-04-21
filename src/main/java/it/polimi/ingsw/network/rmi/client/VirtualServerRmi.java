package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.network.rmi.server.VirtualViewRmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia RMI esposta dal server verso i client.
 * Estende direttamente Remote — non c'è bisogno di VirtualServer come base
 * perché le firme con lo stub dipendono dalla tecnologia e non sono condivisibili.
 */
public interface VirtualServerRmi extends Remote {

    void loginFirstPlayer(String nickname, int numPlayers, VirtualViewRmi clientStub)
            throws RemoteException;

    void login(String nickname, VirtualViewRmi clientStub)
            throws RemoteException;
}
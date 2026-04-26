package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.VirtualServer;
import it.polimi.ingsw.model.GameObserver;
import it.polimi.ingsw.network.rmi.server.VirtualViewRmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia RMI esposta dal server verso i client.
 * Ogni metodo corrisponde a un'azione che il client può invocare sul server.
 */
public interface VirtualServerRmi extends Remote, VirtualServer<VirtualViewRmi> {

    // --- LOBBY ---
    @Override
    void loginFirstPlayer(String nickname, int numPlayers, VirtualViewRmi clientStub) throws RemoteException;
    @Override
    void login(String nickname, VirtualViewRmi clientStub)                            throws RemoteException;

    // --- FASE 1: PIAZZAMENTO TOTEM ---
    @Override
    void placeTotem(String nickname, char boardSpaceLetter)                           throws RemoteException;

    // --- FASE 2: SELEZIONE CARTA ---
    @Override
    void pickCard(String nickname, int cardIndex, boolean fromTop)                    throws RemoteException;
}
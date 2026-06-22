package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.VirtualServer;
import it.polimi.ingsw.model.GameObserver;
import it.polimi.ingsw.network.rmi.server.VirtualViewRmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI-specific server interface exposed by the server to its clients.
 * Each method corresponds to an action a client can invoke on the server over RMI.
 * All methods throw {@link RemoteException} as required by the RMI protocol.
 */
public interface VirtualServerRmi extends Remote, VirtualServer<VirtualViewRmi> {

    // --- LOBBY ---
    @Override
    void loginFirstPlayer(String nickname, int numPlayers, VirtualViewRmi clientStub) throws RemoteException;
    @Override
    void loginToLobby(String nickname, int lobbyId, VirtualViewRmi clientStub)        throws RemoteException;

    @Override
    void requestLobbyList(VirtualViewRmi clientView)                                  throws RemoteException;

    // --- PHASE 1: TOTEM PLACEMENT ---
    @Override
    void placeTotem(String nickname, char boardSpaceLetter)                           throws RemoteException;

    // --- PHASE 2: CARD SELECTION ---
    @Override
    void pickCard(String nickname, int cardIndex, boolean fromTop)                    throws RemoteException;

    /*
    // === SPECTATOR ===
    @Override
    void joinAsSpectator(String nickname, int lobbyId, VirtualViewRmi clientView)     throws RemoteException;
    @Override
    void leaveSpectator(String nickname, VirtualViewRmi clientView)                   throws RemoteException;
    // === END SPECTATOR ===

     */

    // --- HEARTBEAT ---
    /** Client calls this periodically to verify the server is still alive. */
    void ping() throws RemoteException;
}
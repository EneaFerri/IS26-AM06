package it.polimi.ingsw.network.rmi_v0.server;


import it.polimi.ingsw.network.rmi_v0.common.GameView;
import it.polimi.ingsw.network.rmi_v0.common.VirtualView;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface VirtualViewRmi extends Remote, VirtualView {

    void showGameState(GameView view) throws RemoteException;

    void showError(String message) throws RemoteException;
}
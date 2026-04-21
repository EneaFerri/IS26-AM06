package it.polimi.ingsw.network.rmi_v0.common;

import java.rmi.RemoteException;

public interface VirtualView {

    void showGameState(GameView view) throws RemoteException;

    void showError(String message) throws RemoteException;
}
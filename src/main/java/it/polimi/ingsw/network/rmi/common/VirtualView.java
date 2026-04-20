package it.polimi.ingsw.network.common;

import java.rmi.RemoteException;

public interface VirtualView {

    void showGameState(it.polimi.ingsw.network.common.GameView view) throws RemoteException;

    void showError(String message) throws RemoteException;
}
package it.polimi.ingsw.network_rmi_bozza;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * Implementazione di IGameClient lato client.
 * Riceve le callback RMI dal server e le inoltra alla View CLI.
 *
 * È l'oggetto che viene esportato via RMI e passato al server come stub.
 */
public class VirtualView extends UnicastRemoteObject implements IGameClient {

    private final CliView view;

    public VirtualView(CliView view) throws RemoteException {
        super();
        this.view = view;
    }

    @Override
    public void onLoginAccepted(String nickname, int expectedPlayers) throws RemoteException {
        view.showLoginAccepted(nickname, expectedPlayers);
    }

    @Override
    public void onPlayerJoined(String nickname, int currentCount, int expected) throws RemoteException {
        view.showPlayerJoined(nickname, currentCount, expected);
    }

    @Override
    public void onGameStarting(List<String> playerNicknames) throws RemoteException {
        view.showGameStarting(playerNicknames);
    }

    @Override
    public void onError(String message) throws RemoteException {
        view.showError(message);
    }
}

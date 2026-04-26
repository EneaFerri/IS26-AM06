package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.network.rmi.client.VirtualServerRmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementazione RMI del server.
 * Riceve le chiamate remote dai client e le delega a GameController.
 */
public class RmiServer extends UnicastRemoteObject implements VirtualServerRmi {

    private static final String SERVER_NAME = "GameServer";
    private static final int    RMI_PORT    = 1099;

    private final GameController controller;

    public RmiServer(GameController controller) throws RemoteException {
        super();
        this.controller = controller;
    }

    public static void main(String[] args) throws RemoteException {
        Game game = new Game(1);
        GameController controller = new GameController(game);

        RmiServer server = new RmiServer(controller);

        Registry registry = LocateRegistry.createRegistry(RMI_PORT);
        registry.rebind(SERVER_NAME, server);

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║  Server avviato su porta " + RMI_PORT + "    ║");
        System.out.println("║  In attesa di connessioni...      ║");
        System.out.println("╚══════════════════════════════════╝");
    }

    // --- LOBBY ---

    @Override
    public synchronized void loginFirstPlayer(String nickname, int numPlayers,
                                              VirtualViewRmi clientStub) throws RemoteException {
        System.out.println("[RmiServer] loginFirstPlayer: " + nickname);
        controller.loginFirstPlayer(nickname, numPlayers, clientStub);
    }

    @Override
    public synchronized void login(String nickname, VirtualViewRmi clientStub) throws RemoteException {
        System.out.println("[RmiServer] login: " + nickname);
        controller.login(nickname, clientStub);
    }

    // --- FASE 1: PIAZZAMENTO TOTEM ---

    @Override
    public synchronized void placeTotem(String nickname, char boardSpaceLetter) throws RemoteException {
        System.out.println("[RmiServer] placeTotem: " + nickname + " → " + boardSpaceLetter);
        controller.placeTotem(nickname, boardSpaceLetter);
    }

    // --- FASE 2: SELEZIONE CARTA ---

    @Override
    public synchronized void pickCard(String nickname, int cardIndex, boolean fromTop) throws RemoteException {
        System.out.println("[RmiServer] pickCard: " + nickname + " idx=" + cardIndex + " top=" + fromTop);
        controller.pickCard(nickname, cardIndex, fromTop);
    }
}
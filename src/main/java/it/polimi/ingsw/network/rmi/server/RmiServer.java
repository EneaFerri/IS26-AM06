package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.network.rmi.client.RmiClient;
import it.polimi.ingsw.network.rmi.client.VirtualServerRmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Logica del server implementata con tecnologia RMI.
 * Estende UnicastRemoteObject e implementa VirtualServerRmi.
 *
 * Lo stub del client arriva direttamente nei parametri di login/loginFirstPlayer,
 * rendendo la registrazione atomica e correggendo la race condition
 * che limitava l'accesso a 2 client.
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

        VirtualServerRmi server = new RmiServer(controller);

        Registry registry = LocateRegistry.createRegistry(RMI_PORT);
        registry.rebind(SERVER_NAME, server);

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║  Server avviato su porta " + RMI_PORT + "    ║");
        System.out.println("║  In attesa di connessioni...      ║");
        System.out.println("╚══════════════════════════════════╝");
    }

    @Override
    public synchronized void loginFirstPlayer(String nickname, int numPlayers,
                                              VirtualViewRmi clientStub) throws RemoteException {
        System.out.println("[RmiServer] loginFirstPlayer: " + nickname);
        controller.loginFirstPlayer(nickname, numPlayers, clientStub);
    }

    @Override
    public synchronized void login(String nickname, VirtualViewRmi clientStub)
            throws RemoteException {
        System.out.println("[RmiServer] login: " + nickname);
        controller.login(nickname, clientStub);
    }

}

package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.network.rmi.server.VirtualViewRmi;
import it.polimi.ingsw.view.ClientModel;
import it.polimi.ingsw.view.cli.CLIView;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Scanner;

/**
 * Logica del client implementata con tecnologia RMI.
 * Estende UnicastRemoteObject e implementa VirtualViewRmi:
 * questo è il pattern esatto dell'esempio dei prof (RmiClient).
 *
 * Riceve le callback dal server (onLoginAccepted, onPlayerJoined, ecc.)
 * e le inoltra al ClientModel, che notifica la CLIView via Observer.
 */
public class RmiClient extends UnicastRemoteObject implements VirtualViewRmi {

    private static final String SERVER_NAME = "GameServer";
    private static final int    RMI_PORT    = 1099;

    private final VirtualServerRmi server;
    private final ClientModel      model;

    public RmiClient(VirtualServerRmi server, ClientModel model) throws RemoteException {
        super();
        this.server = server;
        this.model  = model;
    }

    // ------------------------------------------------------------------ //
    //  Entry point                                                         //
    // ------------------------------------------------------------------ //

    public static void main(String[] args) throws RemoteException, NotBoundException {
        String host = (args.length > 0) ? args[0] : "localhost";

        Registry registry = LocateRegistry.getRegistry(host, RMI_PORT);
        VirtualServerRmi server = (VirtualServerRmi) registry.lookup(SERVER_NAME);

        ClientModel model = new ClientModel();
        CLIView view = new CLIView();
        model.registerObserver(view);

        new RmiClient(server, model).run();
    }

    // ------------------------------------------------------------------ //
    //  Avvio client                                                        //
    // ------------------------------------------------------------------ //

    private void run() throws RemoteException {
        // 1. Registra questo client presso il server (passa lo stub per le callback)
        this.server.connect(this);

        // 2. Esegui la CLI di login
        this.runLoginCli();

        // 3. Rimani in ascolto delle callback RMI (thread RMI gestisce autonomamente)
        System.out.println("\n[In attesa di aggiornamenti dal server...]");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Client terminato.");
        }
    }

    private void runLoginCli() throws RemoteException {
        Scanner scan = new Scanner(System.in);

        System.out.print("Inserisci il tuo nickname: ");
        String nickname = scan.nextLine().trim();

        System.out.print("Sei il primo giocatore della lobby? (s/n): ");
        String answer = scan.nextLine().trim().toLowerCase();

        if (answer.equals("s")) {
            int numPlayers = 0;
            while (numPlayers < 2 || numPlayers > 5) {
                System.out.print("Quanti giocatori vuoi? (2-5): ");
                try {
                    numPlayers = Integer.parseInt(scan.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Inserisci un numero valido.");
                }
            }
            server.loginFirstPlayer(nickname, numPlayers);
        } else {
            server.login(nickname);
        }
    }

    // ------------------------------------------------------------------ //
    //  VirtualViewRmi — callback chiamate dal server                      //
    // ------------------------------------------------------------------ //

    @Override
    public void onLoginAccepted(String nickname, int expectedPlayers) throws RemoteException {
        model.onLoginAccepted(nickname, expectedPlayers);
    }

    @Override
    public void onPlayerJoined(String nickname, int currentCount, int expected) throws RemoteException {
        model.onPlayerJoined(nickname, currentCount, expected);
    }

    @Override
    public void onGameStarting(List<String> playerNicknames) throws RemoteException {
        model.onGameStarting(playerNicknames);
    }

    @Override
    public void onError(String message) throws RemoteException {
        model.onError(message);
    }
}

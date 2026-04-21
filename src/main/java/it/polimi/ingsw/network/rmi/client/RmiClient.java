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
 * Estende UnicastRemoteObject e implementa VirtualViewRmi.
 *
 * Lo stub di questo oggetto viene passato direttamente nei metodi
 * loginFirstPlayer/login, in un'unica chiamata atomica.
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

    public static void main(String[] args) throws Exception {
        String host = (args.length > 0) ? args[0] : "localhost";

        Registry registry = LocateRegistry.getRegistry(host, RMI_PORT);
        VirtualServerRmi server = (VirtualServerRmi) registry.lookup(SERVER_NAME);

        ClientModel model = new ClientModel();
        CLIView view = new CLIView();
        model.registerObserver(view);

        new RmiClient(server, model).run();
    }

    private void run() throws Exception {
        runLoginCli();

        System.out.println("\n[In attesa di aggiornamenti dal server...]");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Client terminato.");
        }
    }

    private void runLoginCli() throws Exception {
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
            // stub passato direttamente — registrazione e login in una sola chiamata RMI
            server.loginFirstPlayer(nickname, numPlayers, this);
        } else {
            server.login(nickname, this);
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

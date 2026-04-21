package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.model.enums.Age;
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
 * Logica del client RMI.
 * Estende UnicastRemoteObject e implementa VirtualViewRmi:
 * è l'oggetto remoto su cui il server fa le callback.
 *
 * Ogni callback ricevuta viene inoltrata a ClientModel,
 * che notifica CLIView tramite ModelObserver.
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

    public static void main(String[] args) throws RemoteException, NotBoundException {
        String host = (args.length > 0) ? args[0] : "localhost";

        Registry registry = LocateRegistry.getRegistry(host, RMI_PORT);
        VirtualServerRmi server = (VirtualServerRmi) registry.lookup(SERVER_NAME);

        ClientModel model = new ClientModel();
        CLIView view = new CLIView();
        model.registerObserver(view);

        new RmiClient(server, model).run();
    }

    private void run() throws RemoteException {
        runLoginCli();
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
                try { numPlayers = Integer.parseInt(scan.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Numero non valido."); }
            }
            server.loginFirstPlayer(nickname, numPlayers, this);
        } else {
            server.login(nickname, this);
        }
    }

    // ------------------------------------------------------------------ //
    //  VirtualViewRmi — tutte le callback dal server                     //
    // ------------------------------------------------------------------ //

    // --- LOBBY & SETUP ---
    @Override public void onLoginAccepted(String nickname, int expectedPlayers) throws RemoteException {
        model.onLoginAccepted(nickname, expectedPlayers);
    }
    @Override public void onPlayerJoined(String nickname, int currentCount, int expected) throws RemoteException {
        model.onPlayerJoined(nickname, currentCount, expected);
    }
    @Override public void onGameStarting(List<String> playerNicknames) throws RemoteException {
        model.onGameStarting(playerNicknames);
    }
    @Override public void onError(String message) throws RemoteException {
        model.onError(message);
    }

    // --- FASE 1: PIAZZAMENTO TOTEM ---
    @Override public void onTotemPlaced(String nickname, String boardSpaceId) throws RemoteException {
        model.onTotemPlaced(nickname, boardSpaceId);
    }
    @Override public void onInvalidAction(String nicknameTarget, String errorMessage) throws RemoteException {
        model.onInvalidAction(nicknameTarget, errorMessage);
    }

    // --- FASE 2: SELEZIONE CARTE ---
    @Override public void onCardTaken(String nickname, String cardId) throws RemoteException {
        model.onCardTaken(nickname, cardId);
    }
    @Override public void onPlayerUpdated(String nickname) throws RemoteException {
        model.onPlayerUpdated(nickname);
    }

    // --- FINE TURNO GIOCATORE ---
    @Override public void onTurnOrderUpdated(List<String> newOrderedNicknames) throws RemoteException {
        model.onTurnOrderUpdated(newOrderedNicknames);
    }

    // --- FINE ROUND & EVENTI ---
    @Override public void onEventResolved(String eventName, String resultDetails) throws RemoteException {
        model.onEventResolved(eventName, resultDetails);
    }
    @Override public void onBoardUpdated() throws RemoteException {
        model.onBoardUpdated();
    }
    @Override public void onNewEraStarted(Age newEra) throws RemoteException {
        model.onNewEraStarted(newEra);
    }

    // --- FINE PARTITA ---
    @Override public void onGameOver() throws RemoteException {
        model.onGameOver();
    }
}
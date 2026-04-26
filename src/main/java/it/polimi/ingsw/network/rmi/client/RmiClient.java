package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
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

public class RmiClient extends UnicastRemoteObject implements VirtualViewRmi {

    private static final String SERVER_NAME = "GameServer";
    private static final int    RMI_PORT    = 1099;

    private final VirtualServerRmi server;
    private final ClientModel      model;
    private final Scanner          scanner;
    private String myNickname;

    public RmiClient(VirtualServerRmi server, ClientModel model) throws RemoteException {
        super();
        this.server  = server;
        this.model   = model;
        this.scanner = new Scanner(System.in);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        String host = (args.length > 0) ? args[0] : "localhost";

        Registry registry = LocateRegistry.getRegistry(host, RMI_PORT);
        VirtualServerRmi server = (VirtualServerRmi) registry.lookup(SERVER_NAME);

        ClientModel model = new ClientModel();
        CLIView view = new CLIView();
        model.registerObserver(view);

        RmiClient client = new RmiClient(server, model);
        view.setServer(server, client);
        client.run();
    }

    private void run() throws Exception {
        runLoginCli();
        System.out.println("\n[In attesa di aggiornamenti dal server...]");
        try { Thread.currentThread().join(); }
        catch (InterruptedException e) { System.out.println("Client terminato."); }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOGIN CLI  — nuovo flusso multi-lobby
    // ─────────────────────────────────────────────────────────────────────

    private void runLoginCli() throws Exception {
        System.out.print("Inserisci il tuo nickname: ");
        myNickname = scanner.nextLine().trim();

        System.out.print("Vuoi creare una nuova lobby? (s/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
            // Crea sempre una lobby nuova
            int numPlayers = askNumPlayers();
            server.loginFirstPlayer(myNickname, numPlayers, this);
        } else {
            // Chiedi al server le lobby aperte → arriva onLobbyList (o onNoLobbyAvailable)
            server.requestLobbyList(this);
        }
    }

    private int askNumPlayers() {
        int n = 0;
        while (n < 2 || n > 5) {
            System.out.print("Quanti giocatori? (2-5): ");
            try { n = Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Numero non valido."); }
        }
        return n;
    }

    public String getMyNickname() { return myNickname; }

    // ─────────────────────────────────────────────────────────────────────
    //  CALLBACK DA SERVER
    // ─────────────────────────────────────────────────────────────────────

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

    /**
     * Nessuna lobby aperta: chiedi all'utente se vuole crearne una.
     * Questo metodo arriva su un thread RMI → non blocchiamo, lanciamo un thread.
     */
    @Override public void onNoLobbyAvailable() throws RemoteException {
        new Thread(() -> {
            System.out.println("\nNessuna lobby disponibile.");
            System.out.print("Vuoi crearne una nuova? (s/n): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
                try {
                    int n = askNumPlayers();
                    server.loginFirstPlayer(myNickname, n, this);
                } catch (RemoteException e) {
                    System.err.println("[Errore rete] " + e.getMessage());
                }
            } else {
                System.out.println("Arrivederci.");
                System.exit(0);
            }
        }, "lobby-create-thread").start();
    }

    /**
     * Lista lobby disponibili ricevuta dal server.
     * Mostra le opzioni e fa scegliere all'utente.
     */
    @Override public void onLobbyList(List<LobbyManager.LobbyInfo> lobbies) throws RemoteException {
        new Thread(() -> {
            if (lobbies.isEmpty()) {
                try { onNoLobbyAvailable(); }
                catch (RemoteException e) { System.err.println(e.getMessage()); }
                return;
            }
            System.out.println("\nLobby disponibili:");
            lobbies.forEach(l -> System.out.println("  " + l));
            System.out.println("Premi INVIO per unirti alla prima disponibile,");
            System.out.println("oppure digita 'nuova' per crearne una tua.");
            String choice = scanner.nextLine().trim().toLowerCase();
            try {
                if (choice.equals("nuova")) {
                    int n = askNumPlayers();
                    server.loginFirstPlayer(myNickname, n, this);
                } else {
                    server.login(myNickname, this);
                }
            } catch (RemoteException e) {
                System.err.println("[Errore rete] " + e.getMessage());
            }
        }, "lobby-join-thread").start();
    }

    @Override public void onYourTurn(String nickname, GameState phase, String extraInfo) throws RemoteException {
        model.onYourTurn(nickname, phase, extraInfo);
    }
    @Override public void onTotemPlaced(String nickname, String boardSpaceId) throws RemoteException {
        model.onTotemPlaced(nickname, boardSpaceId);
    }
    @Override public void onInvalidAction(String nicknameTarget, String errorMessage) throws RemoteException {
        model.onInvalidAction(nicknameTarget, errorMessage);
    }
    @Override public void onCardTaken(String nickname, String cardId) throws RemoteException {
        model.onCardTaken(nickname, cardId);
    }
    @Override public void onPlayerUpdated(String nickname) throws RemoteException {
        model.onPlayerUpdated(nickname);
    }
    @Override public void onTurnOrderUpdated(List<String> newOrderedNicknames) throws RemoteException {
        model.onTurnOrderUpdated(newOrderedNicknames);
    }
    @Override public void onEventResolved(String eventName, String resultDetails) throws RemoteException {
        model.onEventResolved(eventName, resultDetails);
    }
    @Override public void onBoardUpdated() throws RemoteException {
        model.onBoardUpdated();
    }
    @Override public void onNewEraStarted(Age newEra) throws RemoteException {
        model.onNewEraStarted(newEra);
    }
    @Override public void onGameOver(String results) throws RemoteException {
        model.onGameOver(results);
    }
}
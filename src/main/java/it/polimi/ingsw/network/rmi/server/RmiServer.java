package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.network.rmi.client.VirtualServerRmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Logica del server implementata con tecnologia RMI.
 * Estende UnicastRemoteObject e implementa VirtualServerRmi:
 * questo è il pattern esatto dell'esempio dei prof (RmiServer).
 *
 * Si occupa solo di:
 *  - ricevere le chiamate RMI dai client
 *  - passarle al GameController (che non sa nulla di RMI)
 *  - gestire la lista di VirtualViewRmi per le callback
 */
public class RmiServer extends UnicastRemoteObject implements VirtualServerRmi {

    private static final String SERVER_NAME = "GameServer";
    private static final int    RMI_PORT    = 1099;

    private final GameController controller;
    private final List<VirtualViewRmi> clients = new ArrayList<>();

    public RmiServer(GameController controller) throws RemoteException {
        super();
        this.controller = controller;
    }

    // ------------------------------------------------------------------ //
    //  Entry point                                                         //
    // ------------------------------------------------------------------ //

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

    // ------------------------------------------------------------------ //
    //  VirtualServerRmi — metodi chiamati via RMI dai client              //
    // ------------------------------------------------------------------ //

    @Override
    public void connect(VirtualViewRmi client) throws RemoteException {
        // Registra il client nella lista locale (per le callback RMI)
        // e nel controller (che usa l'interfaccia base VirtualView)
        synchronized (this.clients) {
            this.clients.add(client);
            this.controller.addClient(client);
        }
        System.out.println("[RmiServer] Nuovo client connesso. Totale: " + clients.size());
    }

    @Override
    public void loginFirstPlayer(String nickname, int numPlayers) throws RemoteException {
        System.out.println("[RmiServer] loginFirstPlayer: " + nickname);
        // Recupera la VirtualView del chiamante (l'ultimo client che si è connesso
        // ha già chiamato connect() prima di loginFirstPlayer)
        VirtualViewRmi caller = getCallerView(nickname);
        controller.loginFirstPlayer(nickname, numPlayers, caller);
    }

    @Override
    public void login(String nickname) throws RemoteException {
        System.out.println("[RmiServer] login: " + nickname);
        VirtualViewRmi caller = getCallerView(nickname);
        controller.login(nickname, caller);
    }

    // ------------------------------------------------------------------ //
    //  Utility                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Restituisce la VirtualViewRmi dell'ultimo client connesso che ha il nickname dato.
     * Poiché connect() viene chiamato prima di login/loginFirstPlayer nello stesso
     * client, l'ultimo elemento della lista è il chiamante corrente.
     *
     * Nota: in futuro si può associare client → view con una Map per maggiore robustezza.
     */
    private VirtualViewRmi getCallerView(String nickname) {
        synchronized (this.clients) {
            // L'ultimo client connesso è quello che sta facendo login
            return clients.get(clients.size() - 1);
        }
    }
}

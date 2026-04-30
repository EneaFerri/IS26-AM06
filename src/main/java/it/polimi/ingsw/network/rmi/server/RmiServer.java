package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.CombinedServer;
import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.network.utils.NetworkUtils;
import it.polimi.ingsw.network.rmi.client.VirtualServerRmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * RMI server. Owns (or shares) a {@link LobbyManager} and routes all
 * client calls to it.
 *
 * When started via {@link CombinedServer}, it receives
 * a shared LobbyManager so that RMI and Socket players can join the same games.
 * When started standalone (its own main()), it creates its own LobbyManager.
 */
public class RmiServer extends UnicastRemoteObject implements VirtualServerRmi {

    private static final String SERVER_NAME = "GameServer";
    private static final int    RMI_PORT    = 1099;

    private final LobbyManager lobbyManager;

    // ── Constructors ───────────────────────────────────────────────────────

    /** Constructor used by CombinedServer to inject a shared LobbyManager. */
    public RmiServer(LobbyManager lobbyManager) throws RemoteException {
        super();
        this.lobbyManager = lobbyManager;
    }

    /** Standalone constructor — creates its own LobbyManager. */
    public RmiServer() throws RemoteException {
        this(new LobbyManager());
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    /**
     * Registers this server in the RMI registry.
     * Assumes java.rmi.server.hostname is already set by the caller.
     */
    public void start() throws RemoteException {
        Registry registry = LocateRegistry.createRegistry(RMI_PORT);
        registry.rebind(SERVER_NAME, this);
        System.out.printf("╔══════════════════════════════════╗%n");
        System.out.printf("║  RMI server avviato su porta %-4d ║%n", RMI_PORT);
        System.out.printf("║  IP: %-28s║%n", NetworkUtils.resolveLocalIp());
        System.out.printf("╚══════════════════════════════════╝%n");
    }

    /** Standalone entry point (RMI only, no Socket). */
    public static void main(String[] args) throws RemoteException {
        String ip = NetworkUtils.resolveLocalIp();
        System.setProperty("java.rmi.server.hostname", ip);
        new RmiServer().start();
    }

    // ── VirtualServerRmi ───────────────────────────────────────────────────

    @Override
    public synchronized void loginFirstPlayer(String nickname, int numPlayers,
                                              VirtualViewRmi clientView) throws RemoteException {
        System.out.println("[RmiServer] loginFirstPlayer: " + nickname);
        lobbyManager.createLobby(nickname, numPlayers, clientView);
    }

    @Override
    public synchronized void login(String nickname,
                                   VirtualViewRmi clientView) throws RemoteException {
        System.out.println("[RmiServer] login: " + nickname);
        lobbyManager.joinLobby(nickname, clientView);
    }

    @Override
    public synchronized void requestLobbyList(VirtualViewRmi clientView) throws RemoteException {
        try {
            clientView.onLobbyList(lobbyManager.getOpenLobbies());
        } catch (Exception e) {
            System.err.println("[RmiServer] requestLobbyList: " + e.getMessage());
        }
    }

    @Override
    public synchronized void placeTotem(String nickname,
                                        char boardSpaceLetter) throws RemoteException {
        System.out.println("[RmiServer] placeTotem: " + nickname + " → " + boardSpaceLetter);
        lobbyManager.placeTotem(nickname, boardSpaceLetter);
    }

    @Override
    public synchronized void pickCard(String nickname, int cardIndex,
                                      boolean fromTop) throws RemoteException {
        System.out.println("[RmiServer] pickCard: " + nickname
                + " idx=" + cardIndex + " top=" + fromTop);
        lobbyManager.pickCard(nickname, cardIndex, fromTop);
    }
}
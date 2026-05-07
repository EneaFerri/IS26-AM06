package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.CombinedServer;
import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.network.utils.NetworkUtils;
import it.polimi.ingsw.network.rmi.client.VirtualServerRmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * RMI server. Owns (or shares) a {@link LobbyManager} and routes all
 * client calls to it.
 *
 * When started via {@link CombinedServer}, it receives
 * a shared LobbyManager so that RMI and Socket players can join the same games.
 * When started standalone (its own main()), it creates its own LobbyManager.
 *
 * Heartbeat: for each client that logs in, a ScheduledExecutorService periodically
 * calls clientView.ping(). If RemoteException → client disconnected →
 * lobbyManager.handleDisconnect(nick).
 * RMI is synchronous, so a successful ping() return IS the implicit pong.
 */
public class RmiServer extends UnicastRemoteObject implements VirtualServerRmi {

    private static final String SERVER_NAME   = "GameServer";
    private static final int    RMI_PORT      = 1099;
    private static final int    HEARTBEAT_SEC = 10;

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

    public void start() throws RemoteException {
        Registry registry = LocateRegistry.createRegistry(RMI_PORT);
        registry.rebind(SERVER_NAME, this);
        System.out.printf("╔══════════════════════════════════╗%n");
        System.out.printf("║  RMI server avviato su porta %-4d ║%n", RMI_PORT);
        System.out.printf("║  IP: %-28s║%n", NetworkUtils.resolveLocalIp());
        System.out.printf("╚══════════════════════════════════╝%n");
    }

    public static void main(String[] args) throws RemoteException {
        String ip = NetworkUtils.resolveLocalIp();
        System.setProperty("java.rmi.server.hostname", ip);
        new RmiServer().start();
    }

    // ── Heartbeat  (server → client) ──────────────────────────────────────

    /**
     * Starts a per-client heartbeat that periodically calls clientView.ping().
     * On RemoteException the client is considered gone and LobbyManager is notified.
     */
    private void startClientHeartbeat(String nick, VirtualViewRmi clientView) {
        ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rmi-heartbeat-" + nick);
            t.setDaemon(true);
            return t;
        });
        sched.scheduleAtFixedRate(() -> {
            try {
                clientView.ping();
            } catch (Exception e) {
                System.err.println("[RmiServer] Client " + nick + " heartbeat failed — disconnecting");
                lobbyManager.handleDisconnect(nick);
                sched.shutdown();
            }
        }, HEARTBEAT_SEC, HEARTBEAT_SEC, TimeUnit.SECONDS);
    }

    // ── VirtualServerRmi ───────────────────────────────────────────────────

    @Override
    public synchronized void loginFirstPlayer(String nickname, int numPlayers,
                                              VirtualViewRmi clientView) throws RemoteException {
        System.out.println("[RmiServer] loginFirstPlayer: " + nickname);
        lobbyManager.createLobby(nickname, numPlayers, clientView);
        startClientHeartbeat(nickname, clientView);
    }

    @Override
    public synchronized void login(String nickname,
                                   VirtualViewRmi clientView) throws RemoteException {
        System.out.println("[RmiServer] login: " + nickname);
        lobbyManager.joinLobby(nickname, clientView);
        startClientHeartbeat(nickname, clientView);
    }

    @Override
    public synchronized void loginToLobby(String nickname, int lobbyId,
                                          VirtualViewRmi clientView) throws RemoteException {
        System.out.println("[RmiServer] loginToLobby: " + nickname + " → Lobby #" + lobbyId);
        lobbyManager.joinSpecificLobby(nickname, lobbyId, clientView);
        startClientHeartbeat(nickname, clientView);
    }

    @Override
    public synchronized void requestLobbyList(VirtualViewRmi clientView) throws RemoteException {
        try {
            clientView.onLobbyList(lobbyManager.getActiveLobbies());
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

    // === SPECTATOR ===

    @Override
    public synchronized void joinAsSpectator(String nickname, int lobbyId,
                                             VirtualViewRmi clientView) throws RemoteException {
        System.out.println("[RmiServer] joinAsSpectator: " + nickname + " → Lobby #" + lobbyId);
        lobbyManager.joinAsSpectator(nickname, lobbyId, clientView);
    }

    @Override
    public synchronized void leaveSpectator(String nickname,
                                            VirtualViewRmi clientView) throws RemoteException {
        System.out.println("[RmiServer] leaveSpectator: " + nickname);
        lobbyManager.leaveSpectator(nickname, clientView);
    }

    // === END SPECTATOR ===

    // --- HEARTBEAT ---
    @Override
    public synchronized void ping() throws RemoteException {
        // no-op: the successful return confirms the server is alive
    }
}

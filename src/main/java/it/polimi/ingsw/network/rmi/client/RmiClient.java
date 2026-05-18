package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.GameServerProxy;
import it.polimi.ingsw.network.rmi.server.VirtualViewRmi;
import it.polimi.ingsw.persistence.RankingEntry;
import it.polimi.ingsw.view.ClientModel;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * RMI client adapter.
 *
 * Implements both:
 *   - {@link VirtualViewRmi} (receives server callbacks and forwards them to ClientModel)
 *   - {@link GameServerProxy} (sends player actions to the server via the RMI stub)
 *
 * Heartbeat: a ScheduledExecutorService calls server.ping() every 10s.
 * If RemoteException → server is gone → handleServerDisconnect().
 * RMI is synchronous, so a successful ping() return IS the implicit pong.
 */
public class RmiClient extends UnicastRemoteObject implements VirtualViewRmi, GameServerProxy {

    private static final String SERVER_NAME      = "GameServer";
    private static final int    RMI_PORT         = 1099;
    private static final int    HEARTBEAT_SEC    = 10;

    private final VirtualServerRmi server;
    private final ClientModel      model;

    public RmiClient(VirtualServerRmi server, ClientModel model) throws RemoteException {
        super();
        this.server = server;
        this.model  = model;
    }

    /**
     * Factory: looks up the RMI registry on the given host, creates an RmiClient,
     * and starts the client→server heartbeat.
     */
    public static RmiClient connect(String host, ClientModel model)
            throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(host, RMI_PORT);
        VirtualServerRmi serverStub = (VirtualServerRmi) registry.lookup(SERVER_NAME);
        RmiClient client = new RmiClient(serverStub, model);
        client.startHeartbeat();
        return client;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HEARTBEAT  (client → server)
    // ─────────────────────────────────────────────────────────────────────

    private void startHeartbeat() {
        ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rmi-client-heartbeat");
            t.setDaemon(true);
            return t;
        });
        sched.scheduleAtFixedRate(() -> {
            try {
                server.ping();
            } catch (Exception e) {
                System.err.println("[RmiClient] Server heartbeat failed: " + e.getMessage());
                handleServerDisconnect();
                sched.shutdown();
            }
        }, HEARTBEAT_SEC, HEARTBEAT_SEC, TimeUnit.SECONDS);
    }

    private synchronized void handleServerDisconnect() {
        model.onError("Connessione al server persa. Chiudi e riavvia il client.");
        System.exit(1);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GameServerProxy  (CLIView/GUIView calls these)
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void loginFirstPlayer(String nickname, int numPlayers) throws RemoteException {
        server.loginFirstPlayer(nickname, numPlayers, this);
    }

    @Override
    public void login(String nickname) throws RemoteException {
        server.login(nickname, this);
    }

    @Override
    public void loginToLobby(String nickname, int lobbyId) throws RemoteException {
        server.loginToLobby(nickname, lobbyId, this);
    }

    @Override
    public void requestLobbyList() throws RemoteException {
        server.requestLobbyList(this);
    }

    @Override
    public void placeTotem(String nickname, char boardSpaceLetter) throws RemoteException {
        server.placeTotem(nickname, boardSpaceLetter);
    }

    @Override
    public void pickCard(String nickname, int cardIndex, boolean fromTop) throws RemoteException {
        server.pickCard(nickname, cardIndex, fromTop);
    }

    // === SPECTATOR ===
    @Override
    public void joinAsSpectator(String nickname, int lobbyId) throws RemoteException {
        server.joinAsSpectator(nickname, lobbyId, this);
    }

    @Override
    public void leaveSpectator(String nickname) throws RemoteException {
        server.leaveSpectator(nickname, this);
    }
    // === END SPECTATOR ===

    // ─────────────────────────────────────────────────────────────────────
    //  VirtualViewRmi  (server calls these as callbacks)
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
    @Override public void onNoLobbyAvailable() throws RemoteException {
        model.onNoLobbyAvailable();
    }
    @Override public void onLobbyList(List<LobbyManager.LobbyInfo> lobbies) throws RemoteException {
        model.onLobbyList(lobbies);
    }
    @Override public void onTurnSnapshot(String currentPlayerNick, String boardSummary) throws RemoteException {
        model.onTurnSnapshot(currentPlayerNick, boardSummary);
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
    @Override public void onRankingData(int myRank, int totalEntries,
                                        List<RankingEntry> fullRanking) throws RemoteException {
        model.onRankingData(myRank, totalEntries, fullRanking);
    }
    @Override public void onPlayerDisconnected(String nickname) throws RemoteException {
        model.onPlayerDisconnected(nickname);
    }

    // === SPECTATOR ===
    @Override public void onSpectatorJoined(String currentPlayerNick, String boardSummary) throws RemoteException {
        model.onSpectatorJoined(currentPlayerNick, boardSummary);
    }
    // === END SPECTATOR ===

    // --- HEARTBEAT ---
    @Override public void ping() throws RemoteException {
        // no-op: the successful return confirms this client is alive
    }
}

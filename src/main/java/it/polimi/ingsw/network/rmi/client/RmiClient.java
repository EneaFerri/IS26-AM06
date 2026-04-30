package it.polimi.ingsw.network.rmi.client;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.network.GameServerProxy;
import it.polimi.ingsw.network.rmi.server.VirtualViewRmi;
import it.polimi.ingsw.view.ClientModel;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * RMI client adapter.
 *
 * Implements both:
 *   - {@link VirtualViewRmi} (receives server callbacks and forwards them to ClientModel)
 *   - {@link GameServerProxy} (sends player actions to the server via the RMI stub)
 *
 * The dual role is natural for RMI: the client object IS the remote callback endpoint,
 * and it also holds a reference to the server stub for outgoing calls.
 *
 * Login UI has been moved to {@link it.polimi.ingsw.view.cli.CLIView#doLoginCli()}.
 */
public class RmiClient extends UnicastRemoteObject implements VirtualViewRmi, GameServerProxy {

    private static final String SERVER_NAME = "GameServer";
    private static final int    RMI_PORT    = 1099;

    private final VirtualServerRmi server;
    private final ClientModel      model;

    /**
     * The scanner is obtained from CLIView and passed here to avoid creating
     * a second Scanner on System.in. Only used if this class needs direct input
     * (currently none — login UI is in CLIView).
     */
    public RmiClient(VirtualServerRmi server, ClientModel model) throws RemoteException {
        super();
        this.server = server;
        this.model  = model;
    }

    /**
     * Factory: looks up the RMI registry on the given host and creates an RmiClient
     * connected to it.
     */
    public static RmiClient connect(String host, ClientModel model)
            throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(host, RMI_PORT);
        VirtualServerRmi serverStub = (VirtualServerRmi) registry.lookup(SERVER_NAME);
        return new RmiClient(serverStub, model);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GameServerProxy  (CLIView calls these)
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
    @Override public void onPlayerDisconnected(String nickname) throws RemoteException {
        model.onPlayerDisconnected(nickname);
    }
}
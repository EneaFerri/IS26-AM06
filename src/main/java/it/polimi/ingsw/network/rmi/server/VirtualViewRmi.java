package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.database.RankingEntry;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI specialisation of {@link it.polimi.ingsw.VirtualView}.
 * Extends {@link Remote} and narrows all throws clauses to {@link RemoteException}
 * as required by the RMI protocol.
 * This is the remote object on which the server performs callbacks to each client.
 */
public interface VirtualViewRmi extends Remote, VirtualView {

    // --- LOBBY & SETUP ---
    @Override void onLoginAccepted(String nickname, int expectedPlayers)            throws RemoteException;
    @Override void onPlayerJoined(String nickname, int currentCount, int expected)  throws RemoteException;
    @Override void onGameStarting(List<String> playerNicknames)                     throws RemoteException;
    @Override void onError(String message)                                          throws RemoteException;

    @Override void onNoLobbyAvailable()                                            throws RemoteException;
    @Override void onLobbyList(List<LobbyManager.LobbyInfo> lobbies)              throws RemoteException;

    // --- TURN ---
    @Override void onTurnSnapshot(String currentPlayerNick, String boardSummary)  throws RemoteException;
    @Override void onYourTurn(String nickname, GameState phase, String extraInfo)  throws RemoteException;

    // --- PHASE 1: TOTEM PLACEMENT ---
    @Override void onTotemPlaced(String nickname, String boardSpaceId)              throws RemoteException;
    @Override void onInvalidAction(String nicknameTarget, String errorMessage)      throws RemoteException;

    // --- PHASE 2: CARD SELECTION ---
    @Override void onCardTaken(String nickname, String cardId)                      throws RemoteException;
    @Override void onPlayerUpdated(String nickname)                                 throws RemoteException;

    // --- END OF PLAYER TURN ---
    @Override void onTurnOrderUpdated(List<String> newOrderedNicknames)             throws RemoteException;

    // --- END OF ROUND & EVENTS ---
    @Override void onEventResolved(String eventName, String resultDetails)          throws RemoteException;
    @Override void onBoardUpdated()                                                 throws RemoteException;
    @Override void onNewEraStarted(Age newEra)                                      throws RemoteException;

    // --- END OF GAME ---
    @Override void onGameOver(String results)                                       throws RemoteException;

    // --- DATABASE RANKING ---
    @Override void onRankingData(int myRank, int totalEntries, List<RankingEntry> fullRanking) throws RemoteException;

    // --- DISCONNECTION ---
    @Override void onPlayerDisconnected(String nickname)                             throws RemoteException;
    @Override void onPlayerReplacedByBot(String nickname)                            throws RemoteException;

    /*
    // === SPECTATOR ===
    @Override void onSpectatorJoined(String currentPlayerNick, String boardSummary)  throws RemoteException;
    // === END SPECTATOR ===

     */

    // --- HEARTBEAT ---
    /** Server calls this periodically to verify the client is still alive. */
    void ping() throws RemoteException;
}
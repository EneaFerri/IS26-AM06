package it.polimi.ingsw.network.rmi.server;

import it.polimi.ingsw.VirtualView;
import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.persistence.RankingEntry;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Specializzazione RMI di VirtualView.
 * Aggiunge extends Remote e throws RemoteException a tutti i metodi.
 * È l'oggetto remoto su cui il server fa le callback verso ogni client.
 */
public interface VirtualViewRmi extends Remote, VirtualView {

    // --- LOBBY & SETUP ---
    @Override void onLoginAccepted(String nickname, int expectedPlayers)            throws RemoteException;
    @Override void onPlayerJoined(String nickname, int currentCount, int expected)  throws RemoteException;
    @Override void onGameStarting(List<String> playerNicknames)                     throws RemoteException;
    @Override void onError(String message)                                          throws RemoteException;

    @Override void onNoLobbyAvailable()                                            throws RemoteException;
    @Override void onLobbyList(List<LobbyManager.LobbyInfo> lobbies)              throws RemoteException;

    // --- TURNO ---
    @Override void onTurnSnapshot(String currentPlayerNick, String boardSummary)  throws RemoteException;
    @Override void onYourTurn(String nickname, GameState phase, String extraInfo)  throws RemoteException;

    // --- FASE 1: PIAZZAMENTO TOTEM ---
    @Override void onTotemPlaced(String nickname, String boardSpaceId)              throws RemoteException;
    @Override void onInvalidAction(String nicknameTarget, String errorMessage)      throws RemoteException;

    // --- FASE 2: SELEZIONE CARTE ---
    @Override void onCardTaken(String nickname, String cardId)                      throws RemoteException;
    @Override void onPlayerUpdated(String nickname)                                 throws RemoteException;

    // --- FINE TURNO GIOCATORE ---
    @Override void onTurnOrderUpdated(List<String> newOrderedNicknames)             throws RemoteException;

    // --- FINE ROUND & EVENTI ---
    @Override void onEventResolved(String eventName, String resultDetails)          throws RemoteException;
    @Override void onBoardUpdated()                                                 throws RemoteException;
    @Override void onNewEraStarted(Age newEra)                                      throws RemoteException;

    // --- FINE PARTITA ---
    @Override void onGameOver(String results)                                       throws RemoteException;

    // --- CLASSIFICA DB ---
    @Override void onRankingData(int myRank, int totalEntries, List<RankingEntry> fullRanking) throws RemoteException;

    // --- DISCONNESSIONE ---
    @Override void onPlayerDisconnected(String nickname)                             throws RemoteException;

    // === SPECTATOR ===
    @Override void onSpectatorJoined(String currentPlayerNick, String boardSummary)  throws RemoteException;
    // === END SPECTATOR ===

    // --- HEARTBEAT ---
    /** Server calls this periodically to verify the client is still alive. */
    void ping() throws RemoteException;
}
package it.polimi.ingsw;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;

import java.util.List;

public interface VirtualView {

    // --- LOBBY & SETUP ---
    void onLoginAccepted(String nickname, int expectedPlayers)           throws Exception;
    void onPlayerJoined(String nickname, int currentCount, int expected) throws Exception;
    void onGameStarting(List<String> playerNicknames)                    throws Exception;
    void onError(String message)                                         throws Exception;

    // --- GESTIONE LOBBY MULTIPLE ---
    /** Nessuna lobby aperta: il client deve decidere se crearne una */
    void onNoLobbyAvailable()                                            throws Exception;
    /** Lista lobby aperte da mostrare al client */
    void onLobbyList(List<LobbyManager.LobbyInfo> lobbies)              throws Exception;

    // --- TURNO ---
    void onYourTurn(String nickname, GameState phase, String extraInfo)  throws Exception;

    // --- FASE 1 ---
    void onTotemPlaced(String nickname, String boardSpaceId)             throws Exception;
    void onInvalidAction(String nicknameTarget, String errorMessage)     throws Exception;

    // --- FASE 2 ---
    void onCardTaken(String nickname, String cardId)                     throws Exception;
    void onPlayerUpdated(String nickname)                                throws Exception;

    // --- FINE TURNO ---
    void onTurnOrderUpdated(List<String> newOrderedNicknames)            throws Exception;

    // --- FINE ROUND ---
    void onEventResolved(String eventName, String resultDetails)         throws Exception;
    void onBoardUpdated()                                                throws Exception;
    void onNewEraStarted(Age newEra)                                     throws Exception;

    // --- FINE PARTITA ---
    void onGameOver(String results)                                      throws Exception;
}
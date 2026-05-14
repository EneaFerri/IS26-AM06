package it.polimi.ingsw;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.persistence.RankingEntry;

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
    /**
     * Sent to ALL players at the start of each turn.
     * Contains a full board snapshot so waiting players stay informed.
     *
     * @param currentPlayerNick the player whose turn is starting
     * @param boardSummary      rendered board state (spaces, cards, player stats)
     */
    void onTurnSnapshot(String currentPlayerNick, String boardSummary)   throws Exception;

    /**
     * Sent ONLY to the player whose turn it is.
     * Contains detailed action options (available cards/spaces) and the player's own hand.
     */
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

    // --- CLASSIFICA DB ---
    /**
     * Sent individually to each player (not spectators) after game end.
     * Contains the player's rank in the global historical leaderboard for
     * games with the same number of players.
     */
    void onRankingData(int myRank, int totalEntries, List<RankingEntry> fullRanking) throws Exception;

    // --- DISCONNESSIONE ---
    /** Broadcast to all remaining clients when a player disconnects mid-game. */
    void onPlayerDisconnected(String nickname)                           throws Exception;

    // === SPECTATOR ===
    /**
     * Sent to a client that has just joined a game in progress as a spectator.
     * Provides an immediate board snapshot so the spectator can see the current state.
     * After this callback, the spectator receives onTurnSnapshot for every subsequent turn.
     */
    void onSpectatorJoined(String currentPlayerNick, String boardSummary) throws Exception;
    // === END SPECTATOR ===
}
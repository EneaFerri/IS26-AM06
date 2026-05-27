package it.polimi.ingsw.view;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.database.RankingEntry;

import java.util.List;

/**
 * Interfaccia Observer lato client.
 * Implementata da CLIView (e in futuro GUIView).
 * Riceve notifiche da ClientModel.
 */
public interface ModelObserver {

    // --- LOBBY & SETUP ---
    void onLoginAccepted(String nickname, int expectedPlayers);
    void onPlayerJoined(String nickname, int currentCount, int expected);
    void onGameStarting(List<String> playerNicknames);
    void onError(String message);

    // Lobby multiple
    void onNoLobbyAvailable();
    void onLobbyList(List<LobbyManager.LobbyInfo> lobbies);

    // --- TURNO ---
    /**
     * Broadcast to ALL players at the start of each turn.
     * Waiting players receive the full board state so they stay informed.
     */
    void onTurnSnapshot(String currentPlayerNick, String boardSummary);

    /**
     * Called ONLY on the client of the player who must act.
     * @param nickname  the player who must act (= this client)
     * @param phase     the current game phase
     * @param extraInfo human-readable summary of available options + player's hand
     */
    void onYourTurn(String nickname, GameState phase, String extraInfo);

    // --- FASE 1: PIAZZAMENTO TOTEM ---
    void onTotemPlaced(String nickname, String boardSpaceId);
    void onInvalidAction(String nicknameTarget, String errorMessage);

    // --- FASE 2: SELEZIONE CARTE ---
    void onCardTaken(String nickname, String cardId);
    void onPlayerUpdated(String nickname);

    // --- FINE TURNO GIOCATORE ---
    void onTurnOrderUpdated(List<String> newOrderedNicknames);

    // --- FINE ROUND & EVENTI ---
    void onEventResolved(String eventName, String resultDetails);
    void onBoardUpdated();
    void onNewEraStarted(Age newEra);

    // --- FINE PARTITA ---
    void onGameOver(String results);

    // --- CLASSIFICA DB ---
    /** Sent individually to each player after game end with their global rank and the full leaderboard. */
    void onRankingData(int myRank, int totalEntries, List<RankingEntry> fullRanking);

    // --- DISCONNESSIONE ---
    /** Called when another player disconnects mid-game. */
    void onPlayerDisconnected(String nickname);
    /** Called when a disconnected player is replaced by a bot. */
    void onPlayerReplacedByBot(String nickname);

    // --- SERVER CRASH & RECONNECT ---
    /** Called when the connection to the server is lost. The view should show a waiting message. */
    void onWaitingForServer(String message);
    /** Called when the connection to the server is restored. The view should re-login automatically. */
    void onServerReconnected();

    // === SPECTATOR ===
    /**
     * Called when this client has successfully joined a game as a spectator.
     * Provides the current player's nick and an initial board snapshot.
     */
    void onSpectatorJoined(String currentPlayerNick, String boardSummary);
    // === END SPECTATOR ===
}
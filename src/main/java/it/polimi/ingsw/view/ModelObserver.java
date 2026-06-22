package it.polimi.ingsw.view;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.database.RankingEntry;

import java.util.List;

/**
 * Client-side Observer interface.
 * Implemented by CLIView and GUIView.
 * Receives notifications from {@link ClientModel}.
 */
public interface ModelObserver {

    // --- LOBBY & SETUP ---

    /**
     * Called when the server accepts this client's login request.
     *
     * @param nickname        the accepted nickname
     * @param expectedPlayers total number of players expected in this lobby
     */
    void onLoginAccepted(String nickname, int expectedPlayers);

    /**
     * Called when a new player joins the same lobby.
     *
     * @param nickname     the nickname of the player who joined
     * @param currentCount how many players are currently in the lobby
     * @param expected     how many players the lobby is waiting for
     */
    void onPlayerJoined(String nickname, int currentCount, int expected);

    /**
     * Called when all players have joined and the game is about to start.
     *
     * @param playerNicknames ordered list of all player nicknames
     */
    void onGameStarting(List<String> playerNicknames);

    /**
     * Called when the server reports an error for this client.
     *
     * @param message human-readable error description
     */
    void onError(String message);

    // Multiple lobbies

    /** Called when there are no open lobbies available to join. */
    void onNoLobbyAvailable();

    /**
     * Called with the current list of open lobbies so the player can choose one.
     *
     * @param lobbies list of available lobby descriptors
     */
    void onLobbyList(List<LobbyManager.LobbyInfo> lobbies);

    // --- TURN ---

    /**
     * Broadcast to ALL players at the start of each turn.
     * Waiting players receive the full board state so they stay informed.
     *
     * @param currentPlayerNick nickname of the player whose turn it is
     * @param boardSummary      human-readable board summary
     */
    void onTurnSnapshot(String currentPlayerNick, String boardSummary);

    /**
     * Called ONLY on the client of the player who must act.
     *
     * @param nickname  the player who must act (= this client)
     * @param phase     the current game phase
     * @param extraInfo human-readable summary of available options + player's hand
     */
    void onYourTurn(String nickname, GameState phase, String extraInfo);

    // --- PHASE 1: TOTEM PLACEMENT ---

    /**
     * Called when any player places a totem on an offer space.
     *
     * @param nickname    nickname of the player who placed the totem
     * @param boardSpaceId identifier of the board space where the totem was placed
     */
    void onTotemPlaced(String nickname, String boardSpaceId);

    /**
     * Called when this client attempts an invalid action.
     *
     * @param nicknameTarget the player targeted by the invalid action
     * @param errorMessage   human-readable description of why the action was rejected
     */
    void onInvalidAction(String nicknameTarget, String errorMessage);

    // --- PHASE 2: CARD SELECTION ---

    /**
     * Called when any player picks a card from the board.
     *
     * @param nickname the player who picked the card
     * @param cardId   string identifier of the card that was picked
     */
    void onCardTaken(String nickname, String cardId);

    /**
     * Called when a player's stats change and the view should refresh them.
     *
     * @param nickname the nickname of the player whose state changed
     */
    void onPlayerUpdated(String nickname);

    // --- END OF PLAYER TURN ---

    /**
     * Called when the turn order changes at the end of a player's turn.
     *
     * @param newOrderedNicknames player nicknames in the new turn order
     */
    void onTurnOrderUpdated(List<String> newOrderedNicknames);

    // --- END OF ROUND & EVENTS ---

    /**
     * Called when an event card is resolved at the end of a round.
     *
     * @param eventName     name of the resolved event
     * @param resultDetails human-readable description of the event's outcome
     */
    void onEventResolved(String eventName, String resultDetails);

    /** Called when the board is refreshed with new cards after an era change or event. */
    void onBoardUpdated();

    /**
     * Called when the game advances to a new era.
     *
     * @param newEra the era that has just begun
     */
    void onNewEraStarted(Age newEra);

    // --- GAME OVER ---

    /**
     * Called when the game ends with the final results.
     *
     * @param results human-readable summary of the final scores
     */
    void onGameOver(String results);

    // --- DB RANKING ---

    /**
     * Sent individually to each player after game end with their global rank and the full leaderboard.
     *
     * @param myRank       this player's rank in the historical leaderboard
     * @param totalEntries total number of entries in the leaderboard
     * @param fullRanking  the complete ranked list
     */
    void onRankingData(int myRank, int totalEntries, List<RankingEntry> fullRanking);

    // --- DISCONNECTION ---

    /**
     * Called when another player disconnects mid-game.
     *
     * @param nickname the nickname of the disconnected player
     */
    void onPlayerDisconnected(String nickname);

    /**
     * Called when a disconnected player is replaced by a bot.
     *
     * @param nickname the nickname of the player who was replaced
     */
    void onPlayerReplacedByBot(String nickname);

    // --- SERVER CRASH & RECONNECT ---

    /**
     * Called when the connection to the server is lost. The view should show a waiting message.
     *
     * @param message human-readable description of the connection loss
     */
    void onWaitingForServer(String message);

    /** Called when the connection to the server is restored. The view should re-login automatically. */
    void onServerReconnected();

    /*
    // === SPECTATOR ===

    /**
     * Called when this client has successfully joined a game as a spectator.
     * Provides the current player's nick and an initial board snapshot.
     *
     * @param currentPlayerNick nickname of the player whose turn it currently is
     * @param boardSummary      human-readable snapshot of the current board state

    void onSpectatorJoined(String currentPlayerNick, String boardSummary);
    // === END SPECTATOR ===

     */

}

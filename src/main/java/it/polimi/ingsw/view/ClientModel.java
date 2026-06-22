package it.polimi.ingsw.view;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.database.RankingEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side model.
 * Receives callbacks from RmiClient (via VirtualViewRmi) and notifies
 * registered observers (e.g. CLIView, GUIView) using the Observer pattern.
 * Has no knowledge of RMI or Socket internals.
 */
public class ClientModel {

    private String       myNickname;
    private int          expectedPlayers;
    private List<String> lobbyPlayers = new ArrayList<>();

    private final List<ModelObserver> observers = new ArrayList<>();

    /**
     * Registers an observer to receive game-state notifications.
     *
     * @param observer the observer to register
     */
    public void registerObserver(ModelObserver observer) {
        observers.add(observer);
    }

    // --- LOBBY & SETUP ---

    /**
     * Stores the accepted nickname and expected player count, then notifies observers.
     *
     * @param nickname the accepted nickname
     * @param expected total number of players expected in this lobby
     */
    public void onLoginAccepted(String nickname, int expected) {
        this.myNickname = nickname;
        this.expectedPlayers = expected;
        observers.forEach(o -> o.onLoginAccepted(nickname, expected));
    }

    /**
     * Records the joining player and notifies observers.
     *
     * @param nickname     nickname of the player who joined
     * @param currentCount current number of players in the lobby
     * @param expected     total number of players expected
     */
    public void onPlayerJoined(String nickname, int currentCount, int expected) {
        if (!lobbyPlayers.contains(nickname)) lobbyPlayers.add(nickname);
        observers.forEach(o -> o.onPlayerJoined(nickname, currentCount, expected));
    }

    /**
     * Stores the final player list and notifies observers that the game is starting.
     *
     * @param playerNicknames ordered list of all player nicknames
     */
    public void onGameStarting(List<String> playerNicknames) {
        this.lobbyPlayers = new ArrayList<>(playerNicknames);
        observers.forEach(o -> o.onGameStarting(playerNicknames));
    }

    /**
     * Forwards an error message to all observers.
     *
     * @param message human-readable error description
     */
    public void onError(String message) {
        observers.forEach(o -> o.onError(message));
    }

    /** Notifies observers that no open lobbies are available. */
    public void onNoLobbyAvailable()                               { observers.forEach(o -> o.onNoLobbyAvailable()); }

    /**
     * Forwards the list of available lobbies to all observers.
     *
     * @param lobbies list of available lobby descriptors
     */
    public void onLobbyList(List<LobbyManager.LobbyInfo> lobbies)  { observers.forEach(o -> o.onLobbyList(lobbies)); }

    // --- TURN ---

    /**
     * Forwards a board snapshot broadcast to all observers.
     *
     * @param currentPlayerNick nickname of the player whose turn it is
     * @param boardSummary      human-readable board summary
     */
    public void onTurnSnapshot(String currentPlayerNick, String boardSummary) {
        observers.forEach(o -> o.onTurnSnapshot(currentPlayerNick, boardSummary));
    }

    /**
     * Forwards a "your turn" notification to all observers.
     *
     * @param nickname  the player who must act
     * @param phase     the current game phase
     * @param extraInfo human-readable summary of available options
     */
    public void onYourTurn(String nickname, GameState phase, String extraInfo) {
        observers.forEach(o -> o.onYourTurn(nickname, phase, extraInfo));
    }

    // --- PHASE 1: TOTEM PLACEMENT ---

    /**
     * Notifies observers that a totem was placed on an offer space.
     *
     * @param nickname    nickname of the player who placed the totem
     * @param boardSpaceId identifier of the target board space
     */
    public void onTotemPlaced(String nickname, String boardSpaceId) {
        observers.forEach(o -> o.onTotemPlaced(nickname, boardSpaceId));
    }

    /**
     * Notifies observers that an action was rejected as invalid.
     *
     * @param nicknameTarget the player who attempted the invalid action
     * @param errorMessage   human-readable rejection reason
     */
    public void onInvalidAction(String nicknameTarget, String errorMessage) {
        observers.forEach(o -> o.onInvalidAction(nicknameTarget, errorMessage));
    }

    // --- PHASE 2: CARD SELECTION ---

    /**
     * Notifies observers that a player picked a card.
     *
     * @param nickname the player who picked the card
     * @param cardId   string identifier of the picked card
     */
    public void onCardTaken(String nickname, String cardId) {
        observers.forEach(o -> o.onCardTaken(nickname, cardId));
    }

    /**
     * Notifies observers that a player's stats should be refreshed.
     *
     * @param nickname the player whose state changed
     */
    public void onPlayerUpdated(String nickname) {
        observers.forEach(o -> o.onPlayerUpdated(nickname));
    }

    // --- END OF PLAYER TURN ---

    /**
     * Notifies observers of an updated turn order.
     *
     * @param newOrderedNicknames player nicknames in the new turn order
     */
    public void onTurnOrderUpdated(List<String> newOrderedNicknames) {
        observers.forEach(o -> o.onTurnOrderUpdated(newOrderedNicknames));
    }

    // --- END OF ROUND & EVENTS ---

    /**
     * Notifies observers that an event was resolved.
     *
     * @param eventName     name of the resolved event
     * @param resultDetails human-readable description of the outcome
     */
    public void onEventResolved(String eventName, String resultDetails) {
        observers.forEach(o -> o.onEventResolved(eventName, resultDetails));
    }

    /** Notifies observers that the board was updated (new cards dealt or era change). */
    public void onBoardUpdated() {
        observers.forEach(o -> o.onBoardUpdated());
    }

    /**
     * Notifies observers that a new era has started.
     *
     * @param newEra the era that has just begun
     */
    public void onNewEraStarted(Age newEra) {
        observers.forEach(o -> o.onNewEraStarted(newEra));
    }

    // --- GAME OVER ---

    /**
     * Notifies observers that the game has ended with final results.
     *
     * @param results human-readable summary of the final scores
     */
    public void onGameOver(String results) {
        observers.forEach(o -> o.onGameOver(results));
    }

    // --- DB RANKING ---

    /**
     * Forwards global ranking data to all observers.
     *
     * @param myRank       this player's rank in the historical leaderboard
     * @param totalEntries total number of entries in the leaderboard
     * @param fullRanking  the complete ranked list
     */
    public void onRankingData(int myRank, int totalEntries, List<RankingEntry> fullRanking) {
        observers.forEach(o -> o.onRankingData(myRank, totalEntries, fullRanking));
    }

    // --- DISCONNECTION ---

    /**
     * Notifies observers that another player has disconnected.
     *
     * @param nickname the nickname of the disconnected player
     */
    public void onPlayerDisconnected(String nickname) {
        observers.forEach(o -> o.onPlayerDisconnected(nickname));
    }

    /**
     * Notifies observers that a disconnected player has been replaced by a bot.
     *
     * @param nickname the nickname of the player who was replaced
     */
    public void onPlayerReplacedByBot(String nickname) {
        observers.forEach(o -> o.onPlayerReplacedByBot(nickname));
    }

    /*
    // === SPECTATOR ===

    /**
     * Notifies observers that this client joined as a spectator.
     *
     * @param currentPlayerNick nickname of the player whose turn it currently is
     * @param boardSummary      human-readable snapshot of the current board state

    public void onSpectatorJoined(String currentPlayerNick, String boardSummary) {
        observers.forEach(o -> o.onSpectatorJoined(currentPlayerNick, boardSummary));
    }
    // === END SPECTATOR ===

     */

    // --- SERVER CRASH & RECONNECT ---

    /**
     * Notifies observers that the server connection was lost.
     *
     * @param message human-readable description of the connection loss
     */
    public void onWaitingForServer(String message) {
        observers.forEach(o -> o.onWaitingForServer(message));
    }

    /** Notifies observers that the server connection has been restored. */
    public void onServerReconnected() {
        observers.forEach(o -> o.onServerReconnected());
    }

    // --- Getters ---

    /**
     * Returns this client's nickname as accepted by the server.
     *
     * @return this player's nickname
     */
    public String       getMyNickname()      { return myNickname; }

    /**
     * Returns the number of players expected in the current lobby.
     *
     * @return expected player count
     */
    public int          getExpectedPlayers() { return expectedPlayers; }

    /**
     * Returns an unmodifiable snapshot of the current lobby player list.
     *
     * @return list of nicknames of players currently in the lobby
     */
    public List<String> getLobbyPlayers()    { return List.copyOf(lobbyPlayers); }
}

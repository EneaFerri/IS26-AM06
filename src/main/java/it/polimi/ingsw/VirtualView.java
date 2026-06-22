package it.polimi.ingsw;

import it.polimi.ingsw.controller.LobbyManager;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.database.RankingEntry;

import java.util.List;

/**
 * Server-to-client callback interface.
 *
 * <p>Implemented by every transport adapter (RMI: {@code VirtualViewRmi}, Socket: {@code SocketClientHandler})
 * and by {@link it.polimi.ingsw.controller.Bot} (no-op stubs for a bot that has no UI).</p>
 */
public interface VirtualView {

    // --- LOBBY & SETUP ---

    /**
     * Sent to a player after a successful login or reconnect.
     *
     * @param nickname        the player's confirmed nickname
     * @param expectedPlayers total number of players the lobby is waiting for
     */
    void onLoginAccepted(String nickname, int expectedPlayers)           throws Exception;

    /**
     * Broadcast to all lobby members whenever a new player joins.
     *
     * @param nickname     the nickname of the player who just joined
     * @param currentCount number of players now in the lobby
     * @param expected     total number of players needed to start
     */
    void onPlayerJoined(String nickname, int currentCount, int expected) throws Exception;

    /**
     * Sent to all lobby members when the lobby is full and the game is about to start.
     *
     * @param playerNicknames ordered list of all player nicknames in the game
     */
    void onGameStarting(List<String> playerNicknames)                    throws Exception;

    /**
     * Sent to a single client when a requested action cannot be completed.
     *
     * @param message human-readable error description
     */
    void onError(String message)                                         throws Exception;

    // --- MULTIPLE LOBBY MANAGEMENT ---

    /** Sent when no open lobby exists: the client should decide whether to create one. */
    void onNoLobbyAvailable()                                            throws Exception;

    /**
     * Delivers the current list of open lobbies so the client can choose one to join.
     *
     * @param lobbies list of available lobbies with their status
     */
    void onLobbyList(List<LobbyManager.LobbyInfo> lobbies)              throws Exception;

    // --- TURN ---
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

    // --- PHASE 1: TOTEM PLACEMENT ---

    /**
     * Broadcast when a player successfully places their totem on an offer space.
     *
     * @param nickname     the player who placed the totem
     * @param boardSpaceId identifier of the offer space where the totem was placed
     */
    void onTotemPlaced(String nickname, String boardSpaceId)             throws Exception;

    /**
     * Sent to the player who attempted an invalid action.
     *
     * @param nicknameTarget the player who made the invalid action
     * @param errorMessage   description of why the action was rejected
     */
    void onInvalidAction(String nicknameTarget, String errorMessage)     throws Exception;

    // --- PHASE 2: CARD SELECTION ---

    /**
     * Broadcast when a player picks a card from the board.
     *
     * @param nickname the player who took the card
     * @param cardId   identifier of the card that was taken
     */
    void onCardTaken(String nickname, String cardId)                     throws Exception;

    /**
     * Broadcast when a player's stats change (food, prestige, hand).
     *
     * @param nickname the player whose state was updated
     */
    void onPlayerUpdated(String nickname)                                throws Exception;

    // --- END OF PLAYER TURN ---

    /**
     * Broadcast after all totems are returned to the turn order, announcing the next turn sequence.
     *
     * @param newOrderedNicknames player nicknames in the new turn order
     */
    void onTurnOrderUpdated(List<String> newOrderedNicknames)            throws Exception;

    // --- END OF ROUND ---

    /**
     * Broadcast when an event card is resolved, with details of its effects.
     *
     * @param eventName     name of the resolved event
     * @param resultDetails per-player effect details (formatted for display)
     */
    void onEventResolved(String eventName, String resultDetails)         throws Exception;

    /** Broadcast when the board is refreshed at the start of a new round. */
    void onBoardUpdated()                                                throws Exception;

    /**
     * Broadcast when a new era begins.
     *
     * @param newEra the era that has just started
     */
    void onNewEraStarted(Age newEra)                                     throws Exception;

    // --- END OF GAME ---

    /**
     * Broadcast when the game ends, carrying the final ranking.
     *
     * @param results comma-separated {@code "nickname:points"} pairs, sorted by score descending
     */
    void onGameOver(String results)                                      throws Exception;

    // --- DATABASE RANKING ---
    /**
     * Sent individually to each player (not spectators) after game end.
     * Contains the player's rank in the global historical leaderboard for
     * games with the same number of players.
     */
    void onRankingData(int myRank, int totalEntries, List<RankingEntry> fullRanking) throws Exception;

    // --- DISCONNECTION ---

    /**
     * Broadcast to all remaining clients when a player disconnects mid-game.
     *
     * @param nickname the nickname of the disconnected player
     */
    void onPlayerDisconnected(String nickname)                           throws Exception;

    /**
     * Broadcast to all remaining clients when the disconnected player is replaced by a bot.
     *
     * @param nickname the nickname of the player who was replaced
     */
    void onPlayerReplacedByBot(String nickname)                          throws Exception;

    /*
    // === SPECTATOR ===
    /**
     * Sent to a client that has just joined a game in progress as a spectator.
     * Provides an immediate board snapshot so the spectator can see the current state.
     * After this callback, the spectator receives onTurnSnapshot for every subsequent turn.

    void onSpectatorJoined(String currentPlayerNick, String boardSummary) throws Exception;
    // === END SPECTATOR ===

     */
}
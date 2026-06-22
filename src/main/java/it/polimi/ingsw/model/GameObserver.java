package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;

import java.util.List;

/**
 * Server-side model observer.
 * {@link it.polimi.ingsw.controller.GameController} implements this interface
 * and receives notifications from {@link Game}, then forwards them as
 * {@link it.polimi.ingsw.VirtualView} callbacks to the connected clients.
 */
public interface GameObserver {

    // --- LOBBY & SETUP ---

    /**
     * Notifies that a new player has joined the lobby.
     *
     * @param nickname the nickname of the player who joined
     */
    void onPlayerJoined(String nickname);

    /**
     * Notifies of a player-level error (e.g. duplicate nickname).
     *
     * @param message a human-readable error description
     */
    void onPlayerError(String message);

    /**
     * Notifies that the game has started and the first round is set up.
     */
    void onGameStarted();

    // --- TURN ---

    /**
     * Notifies that it is a specific player's turn.
     * {@code GameController} uses this callback to call {@code onYourTurn} on the correct client.
     *
     * @param nickname the nickname of the player whose turn it is
     * @param phase    the current {@link GameState} phase
     */
    void onTurnStarted(String nickname, GameState phase);

    // --- PHASE 1: TOTEM PLACEMENT ---

    /**
     * Notifies that a player has placed their totem on an offer space.
     *
     * @param nickname     the nickname of the player who placed the totem
     * @param boardSpaceId the identifier (letter) of the occupied board space
     */
    void onTotemPlaced(String nickname, String boardSpaceId);

    /**
     * Notifies that a player attempted an invalid action.
     *
     * @param nicknameTarget the nickname of the player who performed the invalid action
     * @param errorMessage   a description of why the action was invalid
     */
    void onInvalidAction(String nicknameTarget, String errorMessage);

    // --- PHASE 2: CARD SELECTION ---

    /**
     * Notifies that a player has picked a card from the board.
     *
     * @param nickname the nickname of the player who picked the card
     * @param cardId   the string identifier of the picked card
     */
    void onCardTaken(String nickname, String cardId);

    /**
     * Notifies that a player's state (food, prestige, cards) has changed and clients should refresh.
     *
     * @param nickname the nickname of the player whose state was updated
     */
    void onPlayerUpdated(String nickname);

    // --- END OF PLAYER TURN ---

    /**
     * Notifies that the turn order has changed after a player returned their totem.
     *
     * @param newOrderedNicknames the updated turn order as an ordered list of nicknames
     */
    void onTurnOrderUpdated(List<String> newOrderedNicknames);

    // --- END OF ROUND & EVENTS ---

    /**
     * Notifies that an event card has been resolved.
     *
     * @param eventName     the name/type of the resolved event
     * @param resultDetails additional details about the event outcome
     */
    void onEventResolved(String eventName, String resultDetails);

    /**
     * Notifies that the board state has changed and clients should refresh the board view.
     */
    void onBoardUpdated();

    /**
     * Notifies that a new era has begun.
     *
     * @param newEra the {@link Age} that has just started
     */
    void onNewEraStarted(Age newEra);

    // --- END OF GAME ---

    /**
     * Notifies that the game is over and final scores should be computed and displayed.
     */
    void onGameOver();
}

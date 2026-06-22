package it.polimi.ingsw.model.enums;

/**
 * Represents the current phase of the game.
 *
 * <p>The state machine advances through these phases each round, controlling
 * which actions are valid and which notifications are sent to clients.</p>
 */
public enum GameState {

    /** Waiting for all players to connect and the game to start. */
    LOGIN,

    /** Players are placing their totems on the offer-field spaces. */
    OFFER_SPACE_CHOOSE,

    /** Players are picking cards from the board in offer-field order. */
    PICKING_CARD,

    /** A player is drawing an extra card granted by a building card effect. */
    EXTRA_CARD,

    /** Event cards in the bottom row are being resolved. */
    EVENTS,

    /** The game has ended and final scores have been calculated. */
    END
}

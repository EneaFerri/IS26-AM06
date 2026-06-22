package it.polimi.ingsw.model.enums;

/**
 * Represents the age (era) of a card in the game deck.
 *
 * <p>Cards are divided into three historical eras plus a special event-only era.
 * The age determines when a card enters the board and which row it belongs to.</p>
 */
public enum Age {

    /** First era cards, used at the start of the game. */
    Era_I,

    /** Second era cards, introduced mid-game. */
    Era_II,

    /** Third era cards, introduced in the late game. */
    Era_III,

    /** Exclusive era used only for the last two event cards of the game. */
    Last_Event
}

package it.polimi.ingsw.model.enums;

/**
 * Identifies the type of an event card.
 *
 * <p>Each constant corresponds to a distinct event that triggers a specific resolution
 * effect at the end of the picking phase.</p>
 */
public enum EventType {

    /** Hunt event — awards prestige to players with the most Hunter characters. */
    HUNT,

    /** Pictures event — awards prestige to players with the most Artist characters. */
    PICTURES,

    /** Ritual event — awards prestige based on shaman stars; may apply a malus. */
    RITUAL,

    /** Sustenance event — awards or removes food tokens from players. */
    SUSTENANCE
}

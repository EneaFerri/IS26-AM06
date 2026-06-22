package it.polimi.ingsw.model.enums;

/**
 * Identifies the type of a character card.
 *
 * <p>Each constant represents a distinct character role with its own scoring rule
 * or in-game ability.</p>
 */
public enum CharacterType {

    /** Builder character — scores prestige based on building cards owned. */
    BUILDER,

    /** Hunter character — scores prestige based on hunt event outcomes. */
    HUNTER,

    /** Inventor character — scores prestige based on invention tokens collected. */
    INVENTOR,

    /** Shaman character — scores prestige based on shaman stars accumulated. */
    SHAMAN,

    /** Collector character — scores prestige based on complete sets of character cards. */
    COLLECTOR,

    /** Artist character — scores prestige based on pictures event outcomes. */
    ARTIST,

    /**
     * Special type used by building cards that award prestige based on the number
     * of complete character-card sets owned by the player.
     */
    SET_OF_CHAR
}

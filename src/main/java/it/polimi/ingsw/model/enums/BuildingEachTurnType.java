package it.polimi.ingsw.model.enums;

/**
 * Identifies the per-turn effect provided by a {@code BuildingEachTurn} card.
 *
 * <p>Each constant maps to a specific ongoing ability that activates at the start
 * of the relevant game phase every round.</p>
 */
public enum BuildingEachTurnType {

    /** Grants three extra shaman stars during the ritual phase. */
    RITUAL_THREEEXTRASTARS,

    /** Prevents any prestige malus during the ritual phase. */
    RITUAL_NOMALUS,

    /** Doubles the prestige points scored during the ritual phase. */
    RITUAL_DOUBLEPOINTS,

    /** Doubles the prestige points scored by the Builder character. */
    BUILDER_DOUBLEPOINTS,

    /** Grants an extra food token when a complete set of cards is owned. */
    EXTRAFOOD_SET,

    /** Grants an extra food token for each Inventor character card owned. */
    EXTRAFOOD_INVENTORS,

    /** Grants an extra food token when placing a totem on the turn-order tile. */
    EXTRAFOOD_TURNORDER,

    /** Allows the player to draw one extra card during the picking phase. */
    EXTRACARD

}

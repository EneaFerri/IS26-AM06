package it.polimi.ingsw.persistence;

import java.util.List;

/**
 * JSON-serializable snapshot of a Player.
 * All fields are primitives, {@code String}, or lists of such types —
 * no references to model objects.
 */
public record PlayerSnapshot(
        String nickname,
        String totemColor,              // TotemColor.name()

        int nuggets,
        int prestige,

        List<Integer> characterCardIds,
        List<Integer> buildingCardIds,
        List<String>  inventions,       // InventionType.name()

        boolean doublePointForBuilder,
        boolean doublePointForRituals,
        boolean noMalusForRituals,
        boolean extraThreeStars,
        boolean extraFoodOnTurnOrder,
        boolean extraCard,
        boolean setToCheck,
        int     setNumberForExtraFood,
        boolean inventorsToCheck,
        int     foodDiscountFromBuildings
) {}

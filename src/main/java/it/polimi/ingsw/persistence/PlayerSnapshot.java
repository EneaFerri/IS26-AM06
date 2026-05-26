package it.polimi.ingsw.persistence;

import java.util.List;

/**
 * Snapshot JSON-serializzabile di un Player.
 * Tutti i campi sono primitivi, String o liste di tali tipi:
 * nessun riferimento a oggetti del model.
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

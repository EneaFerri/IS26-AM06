package it.polimi.ingsw.persistence;

import java.util.List;

/**
 * Snapshot JSON-serializzabile del Board.
 * Le righe carte sono liste di CardRef (cardId + drawed).
 * Gli spazi offerta tengono traccia del totem posizionato (null = libero).
 */
public record BoardSnapshot(
        List<CardRef> topTribeCards,
        List<CardRef> bottomTribeCards,
        List<CardRef> topBuildingCards,
        List<CardRef> bottomBuildingCards,
        List<SpaceSnap> spaces
) {
    /** Stato di un singolo spazio offerta. totemColor è null se lo spazio è libero. */
    public record SpaceSnap(char letter, String totemColor) {}
}

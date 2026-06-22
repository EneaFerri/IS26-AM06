package it.polimi.ingsw.persistence;

import java.util.List;

/**
 * JSON-serializable snapshot of Board.
 * Card rows are lists of {@link CardRef} (cardId + drawed flag).
 * Offer spaces track the placed totem ({@code null} = space is free).
 */
public record BoardSnapshot(
        List<CardRef> topTribeCards,
        List<CardRef> bottomTribeCards,
        List<CardRef> topBuildingCards,
        List<CardRef> bottomBuildingCards,
        List<SpaceSnap> spaces
) {
    /** State of a single offer space. {@code totemColor} is {@code null} if the space is free. */
    public record SpaceSnap(char letter, String totemColor) {}
}

package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.enums.TotemColor;

/**
 * Represents a player's totem piece used to claim offer spaces on the board.
 *
 * <p>A totem has a fixed color that identifies its owner and tracks the
 * {@link BoardSpace} it currently occupies, or {@code null} when it is
 * on the turn-order track.</p>
 */
public class Totem {

    private final TotemColor color;
    private BoardSpace position;

    /**
     * Creates a new totem with the given color.
     * The totem starts with no position (not placed on the board).
     *
     * @param color the color that uniquely identifies this totem's owner
     */
    public Totem(TotemColor color) {
        this.color = color;
        this.position = null;
    }

    /**
     * Returns the color of this totem.
     *
     * @return the {@link TotemColor} assigned to this totem
     */
    public TotemColor getColor() {
        return color;
    }

    /**
     * Returns the board space this totem currently occupies.
     *
     * @return the current {@link BoardSpace}, or {@code null} if the totem is on the turn-order track
     */
    public BoardSpace getPosition() {
        return position;
    }

    /**
     * Places this totem on the given board space.
     *
     * @param space the {@link BoardSpace} to occupy
     */
    public void place(BoardSpace space) {
        this.position = space;
    }

    /**
     * Removes this totem from its current board space, setting position to {@code null}.
     */
    public void remove() {
        this.position = null;
    }

    /**
     * Returns a string representation of this totem showing its color.
     *
     * @return a string in the form {@code "Color: <color>"}
     */
    public String toString() {
        return "Color: " + color;
    }
}

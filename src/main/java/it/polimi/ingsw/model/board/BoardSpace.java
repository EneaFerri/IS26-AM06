package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.player.Totem;

/**
 * Represents a single offer-field space on the board where a player can place their totem.
 *
 * <p>Each space is identified by a letter and defines how many tribe and building cards
 * a player may pick from the top and bottom rows, along with any food reward.</p>
 */
public class BoardSpace {
    private final char letter;
    private final int numCardsTop;
    private final int numCardsDown;
    private final int nuggets;
    private Totem totem;

    /**
     * Creates a new board space with the given configuration.
     *
     * @param letter      the letter identifier of this space (e.g. 'A', 'B', …)
     * @param numCardsTop number of cards the player may pick from the top row
     * @param numCardsDown number of cards the player may pick from the bottom row
     * @param nuggets     food reward granted when a player occupies this space
     */
    public BoardSpace(char letter, int numCardsTop, int numCardsDown, int nuggets) {
        this.letter = letter;
        this.numCardsTop = numCardsTop;
        this.numCardsDown = numCardsDown;
        this.nuggets = nuggets;
        this.totem = null;
    }

    /**
     * Returns {@code true} if no totem is currently placed on this space.
     *
     * @return {@code true} if the space is free
     */
    public boolean isFree() {
        return totem == null;
    }

    /**
     * Returns the letter identifier of this space.
     *
     * @return the space letter
     */
    public char getLetter() {
        return letter;
    }

    /**
     * Returns the number of top-row cards the occupying player may pick.
     *
     * @return top-row card pick count
     */
    public int getTopCardsNumber() {
        return numCardsTop;
    }

    /**
     * Returns the number of bottom-row cards the occupying player may pick.
     *
     * @return bottom-row card pick count
     */
    public int getBottomCardsNumber() {
        return numCardsDown;
    }

    /**
     * Returns the food reward granted when a player occupies this space.
     *
     * @return food reward amount
     */
    public int getFoodReward() {
        return nuggets;
    }

    /**
     * Returns the totem currently placed on this space, or {@code null} if the space is free.
     *
     * @return the totem on this space, or {@code null}
     */
    public Totem getTotem() {
        return totem;
    }

    /**
     * Places the given totem on this space.
     *
     * @param totem the totem to place
     */
    public void setTotem(Totem totem) {
        this.totem = totem;
    }

    /**
     * Removes the totem from this space, leaving it free.
     */
    public void removeTotem() {
        this.totem = null;
    }

    /**
     * Returns a string representation of this space, including totem color (if present),
     * card pick counts, and food reward.
     *
     * @return string describing this space
     */
    public String toString() {
        if (totem != null) {
            return " {" + letter + ", " + "Totem: " + totem.getColor() + "nTop: " + numCardsTop + " / " + "nDown: " + numCardsDown + ", nugget bonus: " + nuggets + "} ";
        }else{
            return " {" + letter + ", no totem, " +  "nTop: " + numCardsTop + " / " + "nDown: " + numCardsDown + ", nugget bonus: " + nuggets + "} ";
        }

    }
}

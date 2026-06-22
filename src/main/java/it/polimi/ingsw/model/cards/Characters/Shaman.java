package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

/**
 * A Shaman character card.
 *
 * <p>Shamans contribute stars to the Shamanic Ritual event.
 * The player with the most stars receives a prestige bonus, while the player
 * with the fewest suffers a prestige malus.</p>
 */
public class Shaman extends CharacterCard {

    private final int stars;

    /**
     * Creates a new Shaman card.
     *
     * @param cardID  the unique identifier for this card
     * @param cardAge the era this card belongs to
     * @param tag     the minimum number of players for this card to be available
     * @param stars   the number of ritual stars this Shaman contributes
     */
    public Shaman(int cardID, Age cardAge, int tag, int stars) {
        super(cardID, cardAge, tag, CharacterType.SHAMAN);
        this.stars = stars;
    }

    /**
     * Returns the number of ritual stars this Shaman contributes.
     *
     * @return the star count
     */
    public int getStars() {
        return stars;
    }

    /**
     * Returns the number of shaman stars this card contributes for Ritual scoring.
     *
     * @return the star count
     */
    @Override
    public int getShamanStars() {
        return getStars();
    }

    /**
     * Returns a formatted display string for this Shaman card.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        return "Sciamano (" + ageLabel() + ")  [" + "✦".repeat(stars) + "]";
    }

    /**
     * Returns a string representation of this Shaman card.
     *
     * @return a string containing the character info and star count
     */
    public String toString(){
        return super.toString() + ", stars: " + stars + "} ";
    }
}

package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

/**
 * An Artist character card.
 *
 * <p>Artists contribute to the Cave Paintings event: players with enough Artists
 * receive a prestige bonus proportional to their Artist count, while those with
 * too few suffer a flat prestige malus.</p>
 */
public class Artist extends CharacterCard {

    /**
     * Creates a new Artist card.
     *
     * @param cardID  the unique identifier for this card
     * @param cardAge the era this card belongs to
     * @param tag     the minimum number of players for this card to be available
     */
    public Artist(int cardID, Age cardAge, int tag) {
        super(cardID, cardAge, tag, CharacterType.ARTIST);
    }

    /**
     * Returns a formatted display string for this Artist card.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        return "Artista (" + ageLabel() + ")";
    }

    /**
     * Returns a string representation of this Artist card.
     *
     * @return a string containing the character info
     */
    public String toString(){
        return super.toString() + "} ";
    }
}

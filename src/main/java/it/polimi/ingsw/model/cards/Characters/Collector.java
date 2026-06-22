package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

/**
 * A Collector character card.
 *
 * <p>Collectors reduce the food penalty during the Sustenance event
 * by 3 food per Collector card owned.</p>
 */
public class Collector extends CharacterCard {

    /**
     * Creates a new Collector card.
     *
     * @param cardID  the unique identifier for this card
     * @param cardAge the era this card belongs to
     * @param tag     the minimum number of players for this card to be available
     */
    public Collector(int cardID, Age cardAge, int tag) {
        super(cardID, cardAge, tag, CharacterType.COLLECTOR);
    }

    /**
     * Returns a formatted display string for this Collector card.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        return "Raccoglitore (" + ageLabel() + ")  [-3 Cibo al Sostentamento]";
    }

    /**
     * Returns a string representation of this Collector card.
     *
     * @return a string containing the character info
     */
    public String toString(){
        return super.toString() + "} ";
    }
}

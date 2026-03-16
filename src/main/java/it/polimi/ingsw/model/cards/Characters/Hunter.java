package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

public class Hunter extends CharacterCard {

    private final int nuggets;

    public Hunter(int cardID, Age cardAge, int tag, int nuggets) {
        super(cardID, cardAge, tag, CharacterType.HUNTER);
        this.nuggets = nuggets;
    }

    public int getNuggets() {
        return nuggets;
    }
}

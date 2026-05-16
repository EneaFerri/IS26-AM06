package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

public class Collector extends CharacterCard {
    private static final long serialVersionUID = 1L;

    public Collector(int cardID, Age cardAge, int tag) {
        super(cardID, cardAge, tag, CharacterType.COLLECTOR);
    }

    public String toString(){
        return super.toString() + "} ";
    }
}

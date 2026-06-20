package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

public class Collector extends CharacterCard {

    public Collector(int cardID, Age cardAge, int tag) {
        super(cardID, cardAge, tag, CharacterType.COLLECTOR);
    }

    @Override
    public String toDisplayString() {
        return "Raccoglitore (" + ageLabel() + ")  [-3 Cibo al Sostentamento]";
    }

    public String toString(){
        return super.toString() + "} ";
    }
}

package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

public class Artist extends CharacterCard {
    private static final long serialVersionUID = 1L;

    public Artist(int cardID, Age cardAge, int tag) {
        super(cardID, cardAge, tag, CharacterType.ARTIST);
    }

    public String toString(){
        return super.toString() + "} ";
    }
}

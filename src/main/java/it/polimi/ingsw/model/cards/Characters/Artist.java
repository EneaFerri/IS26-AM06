package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

public class Artist extends CharacterCard {

    public Artist(int cardID, Age cardAge, int tag) {
        super(cardID, cardAge, tag, CharacterType.ARTIST);
    }

}

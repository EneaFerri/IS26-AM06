package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.InventionType;

public class Inventor extends CharacterCard {

    private final InventionType invention;

    public Inventor(int cardID, Age cardAge, int tag, InventionType invention) {
        super(cardID, cardAge, tag, CharacterType.INVENTOR);
        this.invention = invention;
    }

    public InventionType getInvention() {
        return invention;
    }
}

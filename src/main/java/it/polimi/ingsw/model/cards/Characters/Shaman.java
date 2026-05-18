package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

public class Shaman extends CharacterCard {

    private final int stars;

    public Shaman(int cardID, Age cardAge, int tag, int stars) {
        super(cardID, cardAge, tag, CharacterType.SHAMAN);
        this.stars = stars;
    }

    public int getStars() {
        return stars;
    }

    @Override
    public int getShamanStars() {
        return getStars();
    }

    public String toString(){
        return super.toString() + ", stars: " + stars + "} ";
    }
}

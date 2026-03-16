package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;

public class Builder extends CharacterCard {

    private final int prestigeValue;
    private final int buildDiscount;

    public Builder(int cardID, Age cardAge, int tag, int prestigeValue, int buildDiscount) {
        super(cardID, cardAge, tag, CharacterType.BUILDER);
        this.prestigeValue = prestigeValue;
        this.buildDiscount = buildDiscount;
    }

    public int getPrestigeValue() {
        return prestigeValue;
    }

    public int getBuildDiscount() {
        return buildDiscount;
    }
}

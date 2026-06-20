package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;

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

    public int getDiscountForBuildings() {
        return buildDiscount;
    }

    @Override
    public int getPrestigeContribution(Player player) {
        if (player.hasDoublePointForBuilder()) {
            return 2 * prestigeValue;
        }
        return prestigeValue;
    }

    @Override
    public String toDisplayString() {
        return "Costruttore (" + ageLabel() + ")  [-" + buildDiscount + " Cibo Edifici | +" + prestigeValue + " PP]";
    }

    public String toString(){
        return super.toString() + ", prestige value:" + prestigeValue + ", build discount " + buildDiscount + "} ";
    }


}

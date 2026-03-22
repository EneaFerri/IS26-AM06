package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.Card;
import it.polimi.ingsw.model.enums.Age;
// import it.polimi.ingsw.model.enums.*; non più necessaria

public abstract class BuildingCard extends Card {
    private final int foodCost;
    private final int prestigePoint;

    public BuildingCard(int cardID, Age cardAge, int foodCost, int prestigePoint) {
        super(cardID, cardAge);
        this.foodCost = foodCost;
        this.prestigePoint = prestigePoint;
    }

    public int getFoodCost() {
        return this.foodCost;
    }

    public int getPrestigePoint() {
        return this.prestigePoint;
    }
}

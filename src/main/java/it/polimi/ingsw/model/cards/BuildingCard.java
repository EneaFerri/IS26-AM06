package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;

public class BuildingCard extends Card {
    private final int foodCost;
    private final int prestigePoint;

    public BuildingCard(int cardID, Age cardAge, int foodCost, int prestigePoint) {
        super(cardID, cardAge);
        this.foodCost = foodCost;
        this.prestigePoint = prestigePoint;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public int getPrestigePoint() {
        return prestigePoint;
    }

}

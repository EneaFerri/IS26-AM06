package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;

public abstract class BuildingCard extends Card {
    int foodCost;
    int prestigePoint;


    public BuildingCard(int cardID, Age cardAge) {
        super(cardID, cardAge);
    }
}

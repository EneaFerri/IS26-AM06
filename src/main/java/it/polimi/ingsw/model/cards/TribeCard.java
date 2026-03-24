package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;

public abstract class TribeCard extends Card {

    //classe d'appoggio per dividire TribeCard dalle BuildingCard

    public TribeCard(int cardID, Age cardAge) {
        super(cardID, cardAge);
    }


}

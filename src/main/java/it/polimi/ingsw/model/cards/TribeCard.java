package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;

public abstract class TribeCard extends Card {

    //classe d'appoggio per dividire TribeCard dalle BuildingCard

    public TribeCard(int cardID, Age cardAge) {
        super(cardID, cardAge);
    }

    public boolean isCharacter(){
        return false;
    }

    public boolean isEvent(){
        return false;
    }

    public boolean isAvailableForPlayers(int numberOfPlayers) {
        return true;
    }

    @Override
    public boolean isBuilding() {
        return false;
    }

    @Override
    public boolean isTribe() {
        return true;
    }


}

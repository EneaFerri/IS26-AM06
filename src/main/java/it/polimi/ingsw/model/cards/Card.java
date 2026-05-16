package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.player.Player;

import java.io.Serializable;

public abstract class Card implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int cardID;
    private final Age cardAge;
    private boolean drawed = false;

    // Costruttore
    public Card(int cardID, Age cardAge){
        this.cardID = cardID;
        this.cardAge = cardAge;
    }

    // Getters
    public int getID(){
        return cardID;
    }

    public Age getAge() {
        return cardAge;
    }

    public boolean isDrawed() {
        return drawed;
    }

    public void markAsDrawed() {
        this.drawed = true;
    }

    public abstract void pick(Player player, Game game);

    public boolean isBuilding() {
        return false;
    }

    public boolean isTribe() {
        return false;
    }

    public boolean isEvent() {return false;}

    public String toString(){
        return "CardId: " + cardID + ", Age: " + cardAge;
    }


}

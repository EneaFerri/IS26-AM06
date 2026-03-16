package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;

public abstract class Card {

    //int cardID;
    //Age cardAge;
    //boolean drawed;
    //boolean discarded;

    private final int cardID;
    private final Age cardAge;

    // Costruttore
    public Card(int cardID, Age cardAge){
        this.cardID = cardID;
        this.cardAge = cardAge;
    }

    // Getters
    public int getCardID(){
        return this.cardID;
    }
    public Age getCardAge() {
        return this.cardAge;
    }

    // Metodo per permettere di applicare effetto delle specifica sottoclasse
    // public abstract void applyEffect();
}

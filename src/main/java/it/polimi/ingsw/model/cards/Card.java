package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;

public abstract class Card {
    //boolean drawed;
    //boolean discarded;

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


    // Metodo per permettere di applicare effetto delle specifica sottoclasse
    // public abstract void applyEffect();
    // MEGLIO NON METTERE IL METODO EFFECT QUI abbiamo deciso
}

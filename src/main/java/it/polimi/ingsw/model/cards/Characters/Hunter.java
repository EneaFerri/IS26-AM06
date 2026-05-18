package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;

public class Hunter extends CharacterCard {

    private final int nuggets; //0 = no icona, 1 = icona

    public Hunter(int cardID, Age cardAge, int tag, int nuggets) {
        super(cardID, cardAge, tag, CharacterType.HUNTER);
        this.nuggets = nuggets;
    }

    public int getNuggets() {
        return nuggets;
    }

    @Override
    public void onAddedToPlayer(Player player) {
        if (getNuggets() > 0) {
            player.addFood(player.getNumHunters());
        }
    }
    public String toString(){
        return super.toString() + ", nuggets: " + nuggets + "} ";
    }
}

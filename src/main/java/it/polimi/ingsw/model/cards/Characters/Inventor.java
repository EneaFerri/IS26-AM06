package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.InventionType;
import it.polimi.ingsw.model.player.Player;

public class Inventor extends CharacterCard {
    private static final long serialVersionUID = 1L;

    private final InventionType invention;

    public Inventor(int cardID, Age cardAge, int tag, InventionType invention) {
        super(cardID, cardAge, tag, CharacterType.INVENTOR);
        this.invention = invention;
    }

    public InventionType getInvention() {
        return invention;
    }

    @Override
    public void onAddedToPlayer(Player player) {
        player.inventorsCountAndCheck(this);
    }

    @Override
    public boolean hasInvention(InventionType inventionType) {
        return getInvention().equals(inventionType);
    }

    public String toString(){
        return super.toString() + "invention: " + invention + "} ";
    }
}

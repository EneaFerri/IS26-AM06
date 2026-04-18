package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.InventionType;
import it.polimi.ingsw.model.player.Player;

public abstract class CharacterCard extends TribeCard {
    private final int tag; //indica il numerino, default 2 gg, altrimenti +3,+4,5

    private final CharacterType characterType;

    public CharacterCard(int cardID, Age cardAge, int tag, CharacterType characterType) {
        super(cardID, cardAge);
        this.tag = tag;
        this.characterType = characterType;
    }

    public int getTag() {
        return tag;
    }

    public CharacterType getCharacterType() {
        return characterType;
    }

    public void onAddedToPlayer(Player player) {
        // default: non fa UN CAZZO come ha detto il nostro broder
    }

    public int getShamanStars() {
        return 0;
    }

    public int getPrestigeContribution(Player player) {
        return 0;
    }

    public boolean hasInvention(InventionType inventionType) {
        return false;
    }

    @Override
    public void pick(Player player, Game game) {
        game.pickCharacterCard(player, this);
    }

    @Override
    public boolean isCharacter(){
        return true;
    }

    @Override
    public boolean isAvailableForPlayers(int numberOfPlayers) {
        return getTag() <= numberOfPlayers;
    }

    public int getDiscountForBuildings(){return 0;}

    public String toString(){
        return " {" + characterType + ", " + super.toString();
    }
}

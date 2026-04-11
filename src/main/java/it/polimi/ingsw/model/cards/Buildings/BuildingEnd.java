package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.cards.CharacterCard;



public class BuildingEnd extends BuildingCard {
    private final CharacterType characterToConsider;
    private final int prestigeEndEffect;

    public BuildingEnd(int cardID, Age cardAge, int foodCost, int prestigePoint,
                       CharacterType characterToConsider, int prestigeEndEffect) {
        super(cardID, cardAge, foodCost, prestigePoint);
        this.characterToConsider = characterToConsider;
        this.prestigeEndEffect = prestigeEndEffect;
    }

    public CharacterType getCharacterToConsider() {
        return characterToConsider;
    }

    public int getPrestigeEndEffect() {
        return prestigeEndEffect;
    }

    @Override
    public int getEndEffectPoints(Player player) {

        //caso speciale per la building con i set
        if(characterToConsider == CharacterType.SET_OF_CHAR) {
            int n = player.countSet();
            return prestigeEndEffect * n;
        }

        int count = 0;

        // conto quanti personaggi del tipo richiesto ha il player
        for (CharacterCard c : player.getCharacterCards()) {
            if (c.getCharacterType() == this.characterToConsider) {
                count++;
            }
        }

        return count * this.prestigeEndEffect;
    }
}

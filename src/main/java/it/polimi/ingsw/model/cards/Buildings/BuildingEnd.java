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



        if(characterToConsider == CharacterType.SET_OF_CHAR) {
            int n = player.countSet();
            return prestigeEndEffect * n;
        }

        int count = 0;

        for (CharacterCard c : player.getCharacterCards()) {
            if (c.getCharacterType() == this.characterToConsider) {
                count++;
            }
        }

        return count * this.prestigeEndEffect;
    }

    @Override
    public String toDisplayString() {
        if (prestigeEndEffect == 0)
            return "Edificio (" + ageLabel() + ")  [costo: " + getFoodCost() + " | PP: " + getPrestigePoint() + "]";
        return "Edificio (" + ageLabel() + ")  [fine partita: +" + prestigeEndEffect
                + " PP per " + charLabel(characterToConsider)
                + " | costo: " + getFoodCost() + " | PP: " + getPrestigePoint() + "]";
    }

    private String charLabel(CharacterType t) {
        return switch (t) {
            case ARTIST      -> "Artisti";
            case BUILDER     -> "Costruttori";
            case COLLECTOR   -> "Raccoglitori";
            case HUNTER      -> "Cacciatori";
            case INVENTOR    -> "Inventori";
            case SHAMAN      -> "Sciamani";
            case SET_OF_CHAR -> "set completi (6 tipi diversi)";
        };
    }

    public String toString(){
        return " { BUILDING_END, " + super.toString() + ", character to consider: " + characterToConsider + ", prestige end effect: " + prestigeEndEffect +  "} ";
    }
}

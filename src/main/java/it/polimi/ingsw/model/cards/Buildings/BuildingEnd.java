package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.cards.CharacterCard;



/**
 * A building card that grants bonus prestige points at the end of the game.
 *
 * <p>The end-game bonus is calculated by counting how many character cards of a
 * specific {@link CharacterType} (or complete sets) the player owns, then
 * multiplying by the configured prestige value.</p>
 */
public class BuildingEnd extends BuildingCard {
    private final CharacterType characterToConsider;
    private final int prestigeEndEffect;

    /**
     * Creates a new end-of-game building card.
     *
     * @param cardID              the unique identifier for this card
     * @param cardAge             the era this card belongs to
     * @param foodCost            the food cost to purchase this building
     * @param prestigePoint       the prestige points granted on purchase
     * @param characterToConsider the character type (or set) scored at end of game
     * @param prestigeEndEffect   the prestige points awarded per matching character
     */
    public BuildingEnd(int cardID, Age cardAge, int foodCost, int prestigePoint,
                       CharacterType characterToConsider, int prestigeEndEffect) {
        super(cardID, cardAge, foodCost, prestigePoint);
        this.characterToConsider = characterToConsider;
        this.prestigeEndEffect = prestigeEndEffect;
    }

    /**
     * Returns the character type considered for end-of-game scoring.
     *
     * @return the {@link CharacterType} (may be {@code SET_OF_CHAR} for complete sets)
     */
    public CharacterType getCharacterToConsider() {
        return characterToConsider;
    }

    /**
     * Returns the prestige points awarded per matching character at end of game.
     *
     * @return the prestige end-effect value
     */
    public int getPrestigeEndEffect() {
        return prestigeEndEffect;
    }

    /**
     * Calculates and returns the prestige points this building contributes at end-of-game scoring.
     *
     * @param player the player who owns this building
     * @return the total prestige contribution based on the player's character cards
     */
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

    /**
     * Returns a formatted display string describing this building's end-game effect for the UI.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        if (prestigeEndEffect == 0)
            return "Edificio (" + ageLabel() + ")  [costo: " + getFoodCost() + " | PP: " + getPrestigePoint() + "]";
        return "Edificio (" + ageLabel() + ")  [fine partita: +" + prestigeEndEffect
                + " PP per " + charLabel(characterToConsider)
                + " | costo: " + getFoodCost() + " | PP: " + getPrestigePoint() + "]";
    }

    /**
     * Returns a localised display label for the given character type, used in UI display strings.
     *
     * @param t the character type to label
     * @return the display label for the character type
     */
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

    /**
     * Returns a string representation of this building card.
     *
     * @return a string containing the building type, base card info, character type, and end effect
     */
    public String toString(){
        return " { BUILDING_END, " + super.toString() + ", character to consider: " + characterToConsider + ", prestige end effect: " + prestigeEndEffect +  "} ";
    }
}

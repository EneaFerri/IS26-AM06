package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;

/**
 * A Builder character card.
 *
 * <p>Builders provide a food discount when purchasing buildings and contribute
 * prestige points at end of game. If the player has a building that doubles
 * Builder prestige, the contribution is multiplied by two.</p>
 */
public class Builder extends CharacterCard {

    private final int prestigeValue;
    private final int buildDiscount;

    /**
     * Creates a new Builder card.
     *
     * @param cardID        the unique identifier for this card
     * @param cardAge       the era this card belongs to
     * @param tag           the minimum number of players for this card to be available
     * @param prestigeValue the prestige points this Builder contributes at end of game
     * @param buildDiscount the food cost discount this Builder provides when purchasing buildings
     */
    public Builder(int cardID, Age cardAge, int tag, int prestigeValue, int buildDiscount) {
        super(cardID, cardAge, tag, CharacterType.BUILDER);
        this.prestigeValue = prestigeValue;
        this.buildDiscount = buildDiscount;
    }

    /**
     * Returns the prestige points this Builder contributes at end of game (before any multiplier).
     *
     * @return the base prestige value
     */
    public int getPrestigeValue() {
        return prestigeValue;
    }

    /**
     * Returns the food cost discount this Builder provides when purchasing buildings.
     *
     * @return the building food discount
     */
    public int getDiscountForBuildings() {
        return buildDiscount;
    }

    /**
     * Returns the prestige contribution of this Builder for the given player,
     * doubling it if the player has a Builder double-points building.
     *
     * @param player the player who owns this card
     * @return the prestige contribution (doubled if the player has the relevant building)
     */
    @Override
    public int getPrestigeContribution(Player player) {
        if (player.hasDoublePointForBuilder()) {
            return 2 * prestigeValue;
        }
        return prestigeValue;
    }

    /**
     * Returns a formatted display string for this Builder card.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        return "Costruttore (" + ageLabel() + ")  [-" + buildDiscount + " Cibo Edifici | +" + prestigeValue + " PP]";
    }

    /**
     * Returns a string representation of this Builder card.
     *
     * @return a string containing the character info, prestige value, and build discount
     */
    public String toString(){
        return super.toString() + ", prestige value:" + prestigeValue + ", build discount " + buildDiscount + "} ";
    }


}

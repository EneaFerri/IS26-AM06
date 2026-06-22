package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;


/**
 * Abstract base class for all building cards.
 *
 * <p>Building cards are purchased by players using food and grant prestige points
 * as well as optional effects that trigger each turn, at game end, or in response
 * to specific events.</p>
 */
public abstract class BuildingCard extends Card {
    private final int foodCost;
    private final int prestigePoint;

    /**
     * Creates a new building card.
     *
     * @param cardID        the unique identifier for this card
     * @param cardAge       the era this card belongs to
     * @param foodCost      the food cost required to purchase this building
     * @param prestigePoint the prestige points granted when this building is purchased
     */
    public BuildingCard(int cardID, Age cardAge, int foodCost, int prestigePoint) {
        super(cardID, cardAge);
        this.foodCost = foodCost;
        this.prestigePoint = prestigePoint;
    }

    /**
     * Returns the food cost required to purchase this building.
     *
     * @return the food cost
     */
    public int getFoodCost() {
        return this.foodCost;
    }

    /**
     * Returns the prestige points granted when this building is purchased.
     *
     * @return the prestige points
     */
    public int getPrestigePoint() {
        return this.prestigePoint;
    }

    /**
     * Applies this building's reaction effect when a specific event occurs.
     * Subclasses override this to implement event-triggered bonuses.
     *
     * @param eventType the type of event that occurred
     * @param player    the player who owns this building
     */
    public void applyEventEffect(EventType eventType,  Player player) {
    }

    /**
     * Called when this building card is added to a player's collection.
     * Subclasses override this to apply immediate or recurring effects.
     *
     * @param player the player who received this card
     */
    public void onAddedToPlayer(Player player) {
    }

    /**
     * Returns the prestige points this building contributes at end-of-game scoring.
     *
     * @param player the player who owns this building
     * @return the end-game prestige contribution (default 0)
     */
    public int getEndEffectPoints(Player player) {
        return 0;
    }

    /**
     * Registers the building card purchase in the game when a player picks it.
     *
     * @param player the player who picked this card
     * @param game   the current game instance
     */
    @Override
    public void pick(Player player, Game game) {
        game.pickBuildingCard(player, this);
    }

    /**
     * Returns {@code true} since this is a building card.
     *
     * @return {@code true}
     */
    @Override
    public boolean isBuilding() {
        return true;
    }

    /**
     * Returns a string representation of this building card.
     *
     * @return a string containing the base card info plus food cost and prestige points
     */
    public String toString(){
        return super.toString() + ", food cost: " + foodCost + ", prestige point: " + prestigePoint;
    }
}

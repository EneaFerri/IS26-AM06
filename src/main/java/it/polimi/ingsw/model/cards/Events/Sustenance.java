package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Buildings.BuildingEvent;
import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

/**
 * The Sustenance event card.
 *
 * <p>At resolution, each player must pay 1 food per character card they own.
 * Building event cards reacting to SUSTENANCE (e.g., Collector discounts) are applied first.
 * Players who cannot cover the full cost lose prestige proportional to the food shortfall.</p>
 */
public class Sustenance extends EventCard {
    private static final int FOOD_PRICE = 1;
    private final int prestigeMalus;

    /**
     * Creates a new Sustenance event card.
     *
     * @param cardID        the unique identifier for this card
     * @param cardAge       the era this card belongs to
     * @param prestigeMalus the prestige lost per food unit that cannot be paid
     */
    public Sustenance(int cardID, Age cardAge, int prestigeMalus) {
        super(cardID, cardAge, EventType.SUSTENANCE);
        this.prestigeMalus = prestigeMalus;
    }

    /**
     * Returns the prestige lost per food unit that cannot be paid during this event.
     *
     * @return the prestige malus per missing food
     */
    public int getPrestigeMalus() {
        return prestigeMalus;
    }

    /**
     * Resolves the Sustenance event: applies building discounts, deducts food, and applies
     * prestige malus for any food shortfall.
     *
     * @param players the list of all players in the game
     */
    @Override
    public void resolve(List<Player> players) {
        for (Player player : players) {

            for(BuildingCard bCard : player.getBuildingCards()){
                bCard.applyEventEffect(EventType.SUSTENANCE, player);
            }

            int totalCharacters = player.getCharacterCards().size();

            int totalCost = Math.max(0, (FOOD_PRICE * totalCharacters) - player.getTotalFoodDiscount());

            player.resetBuildingFoodDiscount();

            if (player.getFood() >= totalCost) {
                player.removeFood(totalCost);
            } else {
                int availableFood = player.getFood();
                int missingFood = totalCost - availableFood;

                if (availableFood > 0) {
                    player.removeFood(availableFood);
                }

                player.removePrestige(missingFood * prestigeMalus);
            }
        }
    }

    /**
     * Returns a formatted display string for this Sustenance event card.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        return "⚑ Sostentamento (" + ageLabel() + ")  [-" + prestigeMalus + " PP per Personaggio non sfamato]";
    }

    /**
     * Returns a string representation of this Sustenance event card.
     *
     * @return a string containing the event info, food price, and prestige malus
     */
    public String toString(){
        return super.toString() + "food price" + FOOD_PRICE + "prestigeMalus" + prestigeMalus + "} ";
    }
}

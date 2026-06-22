package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Buildings.BuildingEvent;
import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.CharacterType;

import java.util.List;

/**
 * The Hunt event card.
 *
 * <p>When resolved, each player gains 1 food and a prestige bonus for every
 * Hunter character card they own. Building event cards that react to HUNT
 * are also triggered before the Hunter bonuses are applied.</p>
 */
public class Hunt extends EventCard {
    private static final int FOOD_BONUS = 1;
    private final int prestigeBonus;

    /**
     * Creates a new Hunt event card.
     *
     * @param cardID        the unique identifier for this card
     * @param cardAge       the era this card belongs to
     * @param prestigeBonus the prestige points awarded per Hunter during this event
     */
    public Hunt(int cardID, Age cardAge, int prestigeBonus) {
        super(cardID, cardAge, EventType.HUNT);
        this.prestigeBonus = prestigeBonus;
    }

    /**
     * Returns the prestige bonus awarded per Hunter when this event resolves.
     *
     * @return the prestige bonus per Hunter
     */
    public int getPrestigeBonus() {
        return prestigeBonus;
    }

    /**
     * Resolves the Hunt event: triggers building bonuses, then awards food and prestige per Hunter.
     *
     * @param players the list of all players in the game
     */
    @Override
    public void resolve(List<Player> players) {
        for (Player player : players) {
            int hunters = 0;

            for(BuildingCard bCard : player.getBuildingCards()){
                bCard.applyEventEffect(EventType.HUNT, player);
            }

            for (CharacterCard card : player.getCharacterCards()) {
                if (card.getCharacterType() == CharacterType.HUNTER) {
                    hunters++;
                }
            }

            player.addFood(hunters * FOOD_BONUS);
            player.addPrestige(hunters * prestigeBonus);
        }
    }

    /**
     * Returns a formatted display string for this Hunt event card.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        return "⚑ Caccia (" + ageLabel() + ")  [+1 Cibo +" + prestigeBonus + " PP per Cacciatore]";
    }

    /**
     * Returns a string representation of this Hunt event card.
     *
     * @return a string containing the event info, food bonus, and prestige bonus
     */
    public String toString(){
        return super.toString() + "food bonus" + FOOD_BONUS + "prestige bonus" + prestigeBonus + "} ";
    }
}

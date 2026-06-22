package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

/**
 * The Shamanic Ritual event card.
 *
 * <p>At resolution, the player with the most shaman stars gains a prestige bonus;
 * the player with the fewest stars suffers a prestige malus. Buildings and
 * special flags (double bonus, no malus, extra stars) modify the calculation.</p>
 */
public class Ritual extends EventCard {
    private final int maxBonus;
    private final int maxMalus;

    /**
     * Creates a new Shamanic Ritual event card.
     *
     * @param cardID   the unique identifier for this card
     * @param cardAge  the era this card belongs to
     * @param maxBonus the prestige bonus awarded to the player with the most shaman stars
     * @param maxMalus the prestige malus applied to the player with the fewest shaman stars
     */
    public Ritual(int cardID, Age cardAge, int maxBonus, int maxMalus) {
        super(cardID, cardAge, EventType.RITUAL);
        this.maxBonus = maxBonus;
        this.maxMalus = maxMalus;
    }

    /**
     * Returns the prestige bonus awarded to the player with the most shaman stars.
     *
     * @return the maximum bonus
     */
    public int getMaxBonus() {
        return maxBonus;
    }

    /**
     * Returns the prestige malus applied to the player with the fewest shaman stars.
     *
     * @return the maximum malus
     */
    public int getMaxMalus() {
        return maxMalus;
    }

    /**
     * Resolves the Shamanic Ritual event: awards the bonus to the highest scorer and
     * applies the malus to the lowest scorer.
     *
     * @param players the list of all players in the game
     */
    @Override
    public void resolve(List<Player> players) {
        int maxStars = -1;
        int minStars = Integer.MAX_VALUE;

        for (Player player : players) {
            int stars = getRitualStars(player);

            if (stars > maxStars) {
                maxStars = stars;
            }
            if (stars < minStars) {
                minStars = stars;
            }
        }

        for (Player player : players) {
            if (getRitualStars(player) == maxStars) {
                if(!player.doublePointForRituals){
                    player.addPrestige(maxBonus);
                }else{
                    player.addPrestige(maxBonus*2);
                }
            }
        }
        for (Player player : players) {
            if (getRitualStars(player) == minStars) {
                if(!player.noMalusForRituals){
                    player.removePrestige(maxMalus);
                }
            }
        }
    }

    /**
     * Calculates the effective star count for a player, including any extra-star building bonus.
     *
     * @param player the player whose star total to calculate
     * @return the effective shaman star count
     */
    private int  getRitualStars(Player player) {
        int stars = player.getStarsFromShamans();
        if (player.extraThreeStars) {
            stars = stars +3;
        }
        return stars;
    }

    /**
     * Returns a formatted display string for this Shamanic Ritual event card.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        return "⚑ Rituale Sciamanico (" + ageLabel() + ")  [max: +" + maxBonus + " PP | min: -" + maxMalus + " PP]";
    }

    /**
     * Returns a string representation of this Shamanic Ritual event card.
     *
     * @return a string containing the event info and bonus/malus values
     */
    public String toString(){
        return super.toString() + "maxBonus" + maxBonus + "maxMalus" + maxMalus + "} ";
    }
}

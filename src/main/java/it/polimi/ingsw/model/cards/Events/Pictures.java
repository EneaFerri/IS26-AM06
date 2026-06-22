package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Buildings.BuildingEvent;
import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

/**
 * The Cave Paintings event card.
 *
 * <p>Players who meet the minimum Artist threshold receive a prestige bonus
 * proportional to their Artist count. Players below the threshold suffer a
 * flat prestige malus. Building event cards reacting to PICTURES are also triggered.</p>
 */
public class Pictures extends EventCard {
    // Named minimumArtists rather than a generic "value" for readability
    private final int minimumArtists;
    private final int prestigeMalus;
    private final int prestigeBonus;

    /**
     * Creates a new Cave Paintings event card.
     *
     * @param cardID         the unique identifier for this card
     * @param cardAge        the era this card belongs to
     * @param minimumArtists the minimum number of Artists required to receive the bonus
     * @param prestigeMalus  the prestige lost by players below the Artist threshold
     * @param prestigeBonus  the prestige gained per Artist for players meeting the threshold
     */
    public Pictures(int cardID, Age cardAge, int minimumArtists, int prestigeMalus, int prestigeBonus) {
        super(cardID, cardAge, EventType.PICTURES);
        this.minimumArtists = minimumArtists;
        this.prestigeMalus = prestigeMalus;
        this.prestigeBonus = prestigeBonus;
    }

    /**
     * Returns the minimum number of Artists required for the bonus.
     *
     * @return the Artist threshold
     */
    public int getMinimumArtists() {
        return minimumArtists;
    }

    /**
     * Returns the prestige lost by players below the Artist threshold.
     *
     * @return the prestige malus
     */
    public int getPrestigeMalus() {
        return prestigeMalus;
    }

    /**
     * Returns the prestige gained per Artist for players who meet the threshold.
     *
     * @return the prestige bonus per Artist
     */
    public int getPrestigeBonus() {
        return prestigeBonus;
    }

    /**
     * Resolves the Cave Paintings event: triggers building bonuses, then applies prestige bonus or malus.
     *
     * @param players the list of all players in the game
     */
    @Override
    public void resolve(List<Player> players) {
        for (Player player : players) {

            for (BuildingCard bCard : player.getBuildingCards()) {
                bCard.applyEventEffect(EventType.PICTURES, player);
            }

            if (player.getNumArtists() >= this.minimumArtists) {
                player.addPrestige(this.prestigeBonus * player.getNumArtists());
            } else {
                player.removePrestige(this.prestigeMalus);
            }

        }
    }

    /**
     * Returns a formatted display string for this Cave Paintings event card.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        return "⚑ Pitture Rupestri (" + ageLabel() + ")  [soglia: " + minimumArtists + " Art. | ≥: +"
                + prestigeBonus + " PP/Art. | <: -" + prestigeMalus + " PP]";
    }

    /**
     * Returns a string representation of this Cave Paintings event card.
     *
     * @return a string containing the event info and scoring thresholds
     */
    public String toString(){
        return super.toString() + "minimumArtists" + minimumArtists + "prestigeMalus" + prestigeMalus + "prestigeBonus" + prestigeBonus + "} ";
    }
}

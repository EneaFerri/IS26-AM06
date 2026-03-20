package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

public class Ritual extends EventCard {
    private final int maxBonus;
    private final int maxMalus;

    public Ritual(int cardID, Age cardAge, int maxBonus, int maxMalus) {
        super(cardID, cardAge, EventType.RITUAL);
        this.maxBonus = maxBonus;
        this.maxMalus = maxMalus;
    }

    public int getMaxBonus() {
        return maxBonus;
    }

    public int getMaxMalus() {
        return maxMalus;
    }

    @Override
    public void resolve(List<Player> players) { //TODO: da implementare la applyEventEffect una volta fatta su ritual nelle buildingCard
        int maxStars = -1;
        int minStars = Integer.MAX_VALUE;

        for (Player player : players) {
            int stars = player.getStarsFromShamans();
            if (stars > maxStars) {
                maxStars = stars;
            }
            if (stars < minStars) {
                minStars = stars;
            }
        }

        for (Player player : players) {
            if (player.getStarsFromShamans() == maxStars) player.addPrestige(maxBonus);
        }
        for (Player player : players) {

            if (player.getStarsFromShamans() == minStars) player.removePrestige(maxMalus);
        }
    }
}

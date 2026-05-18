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

    private int  getRitualStars(Player player) {
        int stars = player.getStarsFromShamans();
        if (player.extraThreeStars) {
            stars = stars +3;
        }
        return stars;
    }

    public String toString(){
        return super.toString() + "maxBonus" + maxBonus + "maxMalus" + maxMalus + "} ";
    }
}

package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

public class Sustenance extends EventCard {
    private static final int FOOD_PRICE = 1;
    private final int prestigeMalus;

    public Sustenance(int cardID, Age cardAge, int prestigeMalus) {
        super(cardID, cardAge, EventType.SUSTENANCE);
        this.prestigeMalus = prestigeMalus;
    }

    public int getPrestigeMalus() {
        return prestigeMalus;
    }

    @Override
    public void resolve(List<Player> players) {
        for (Player player : players) {
            // mi calcolo il numero tot di carte per comodità
            int totalCharacters = player.getCharacterCards().size();

            int totalCost = Math.max(0, (FOOD_PRICE * totalCharacters) - player.getTotalFoodDiscount());

            if (player.getFood() >= totalCost) {
                player.removeFood(totalCost); //il giocatore è ricco di SCIBO, tipo il coppe
            } else {
                int availableFood = player.getFood();
                int missingFood = totalCost - availableFood;

                if (availableFood > 0) {
                    player.removeFood(availableFood);
                }

                player.removePrestige(missingFood * prestigeMalus); /*quando finisci il cibo paghi tanti punti quanti
                                                                    indicati sulla carta + il numero di cibi mancanti*/
            }
        }
    }
}

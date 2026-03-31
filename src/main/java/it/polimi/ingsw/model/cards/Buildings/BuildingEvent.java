package it.polimi.ingsw.model.cards.Buildings;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.cards.CharacterCard;

public class BuildingEvent extends BuildingCard {
    private final EventType eventToRespond;
    private final CharacterType characterToConsider;

    public BuildingEvent(int cardID, Age cardAge, int foodCost, int prestigePoint,
                         EventType eventToRespond, CharacterType characterToConsider) {
        super(cardID, cardAge, foodCost, prestigePoint);
        this.eventToRespond = eventToRespond;
        this.characterToConsider = characterToConsider;
    }

    @Override
    public void applyEventEffect(EventType event, Player player) {
        // Se l'evento in corso non è quello a cui questo edificio risponde, ci fermiamo subito
        if (this.eventToRespond == null || this.eventToRespond != event) return;

        if (event == EventType.SUSTENANCE) {
            int moreDiscount = 0;
            for (CharacterCard c : player.getCharacterCards()) {
                if (c.getCharacterType().equals(characterToConsider)) {
                    moreDiscount++;
                }
            }
            player.addBuildingFoodDiscount(moreDiscount);
        }

        if (event == EventType.PICTURES) {
            int n = player.getNumArtists();
            player.addFood(n);
        }

        if (event == EventType.HUNT) {
            int n = player.getNumHunters();
            player.addFood(n);
            player.addPrestige(n);
        }


    }
}


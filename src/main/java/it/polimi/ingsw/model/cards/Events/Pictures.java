package it.polimi.ingsw.model.cards.Events;

import it.polimi.ingsw.model.cards.BuildingCard;
import it.polimi.ingsw.model.cards.Buildings.BuildingEvent;
import it.polimi.ingsw.model.cards.EventCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

public class Pictures extends EventCard {
    private final int minimumArtists; // meglio minimumArtist di value, almeno nel codice riusciamo a capire cosa sono
    private final int prestigeMalus;
    private final int prestigeBonus;

    public Pictures(int cardID, Age cardAge, int minimumArtists, int prestigeMalus, int prestigeBonus) {
        super(cardID, cardAge, EventType.PICTURES);
        this.minimumArtists = minimumArtists;
        this.prestigeMalus = prestigeMalus;
        this.prestigeBonus = prestigeBonus;
    }

    public int getMinimumArtists() {
        return minimumArtists;
    }

    public int getPrestigeMalus() {
        return prestigeMalus;
    }

    public int getPrestigeBonus() {
        return prestigeBonus;
    }

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

    @Override
    public String toDisplayString() {
        return "⚑ Pitture Rupestri (" + ageLabel() + ")  [soglia: " + minimumArtists + " Art. | ≥: +"
                + prestigeBonus + " PP/Art. | <: -" + prestigeMalus + " PP]";
    }

    public String toString(){
        return super.toString() + "minimumArtists" + minimumArtists + "prestigeMalus" + prestigeMalus + "prestigeBonus" + prestigeBonus + "} ";
    }
}
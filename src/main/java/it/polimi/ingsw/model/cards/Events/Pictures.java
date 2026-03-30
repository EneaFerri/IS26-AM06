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

            // Attiviamo gli edifici (se ne hanno) che reagiscono a PICTURES
            for (BuildingCard bCard : player.getBuildingCards()) {
                bCard.applyEventEffect(EventType.PICTURES, player);
            }

            // 2 EFFETTO CARTA: Risolviamo l'evento Pictures per giocatore
            if (player.getNumArtists() >= this.minimumArtists) {
                // punti bonus x num artisti
                player.addPrestige(this.prestigeBonus * player.getNumArtists());
            } else {
                // tipo se non ha abbastanza artisti subisce malus
                player.removePrestige(this.prestigeMalus);
            }

        }
    }
}
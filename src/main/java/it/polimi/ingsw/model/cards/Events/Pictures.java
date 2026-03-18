package it.polimi.ingsw.model.cards.Events;

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
            if (player.getNumArtists() >= minimumArtists) {
                player.addPrestige(prestigeBonus * player.getNumArtists()); // PP per ogni artista
            } else {
                player.removePrestige(prestigeMalus);
            }
        }
    }
}

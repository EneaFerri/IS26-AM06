package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

public abstract class EventCard extends TribeCard {

    public EventCard(int cardID, Age cardAge) {
        super(cardID, cardAge);
    }

    public abstract void resolve(List<Player> players);
}

package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

public abstract class EventCard extends TribeCard {

    private final EventType eventType;

    public EventCard(int cardID, Age cardAge, EventType eventType) {
        super(cardID, cardAge);
        this.eventType = eventType;
    }

    public EventType getType() {
        return eventType;
    }

    public abstract void resolve(List<Player> players);
}

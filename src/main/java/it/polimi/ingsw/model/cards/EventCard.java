package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

public abstract class EventCard extends TribeCard {
    private static final long serialVersionUID = 1L;

    private final EventType eventType;

    public EventCard(int cardID, Age cardAge, EventType eventType) {
        super(cardID, cardAge);
        this.eventType = eventType;
    }

    public EventType getType() {
        return eventType;
    }

    public abstract void resolve(List<Player> players);

    @Override
    public void pick(Player player, Game game) {
        throw new IllegalArgumentException("Event cards cannot be picked by players");
    }

    @Override
    public boolean isCharacter() {
        return false;
    }

    @Override
    public boolean isEvent() {
        return true;
    }

    public String toString(){
        return " {Event:" + eventType + ", " +super.toString();
    }
}

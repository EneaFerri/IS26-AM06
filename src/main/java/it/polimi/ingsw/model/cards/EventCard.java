package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.player.Player;

import java.util.List;

/**
 * Abstract base class for all event cards.
 *
 * <p>Event cards are placed in the tribe rows and trigger effects that apply
 * to all players when resolved at the end of a game round. Unlike character
 * cards, event cards cannot be picked by individual players.</p>
 */
public abstract class EventCard extends TribeCard {

    private final EventType eventType;

    /**
     * Creates a new event card.
     *
     * @param cardID    the unique identifier for this card
     * @param cardAge   the era this card belongs to
     * @param eventType the type of event this card represents
     */
    public EventCard(int cardID, Age cardAge, EventType eventType) {
        super(cardID, cardAge);
        this.eventType = eventType;
    }

    /**
     * Returns the event type of this card.
     *
     * @return the {@link EventType}
     */
    public EventType getType() {
        return eventType;
    }

    /**
     * Resolves this event card's effect, applying it to all players.
     *
     * @param players the list of all players in the game
     */
    public abstract void resolve(List<Player> players);

    /**
     * Throws {@link IllegalArgumentException} since event cards cannot be picked by players.
     *
     * @param player the player attempting to pick the card (unused)
     * @param game   the current game instance (unused)
     * @throws IllegalArgumentException always, since event cards are not pickable
     */
    @Override
    public void pick(Player player, Game game) {
        throw new IllegalArgumentException("Event cards cannot be picked by players");
    }

    /**
     * Returns {@code false} since this is an event card, not a character card.
     *
     * @return {@code false}
     */
    @Override
    public boolean isCharacter() {
        return false;
    }

    /**
     * Returns {@code true} since this is an event card.
     *
     * @return {@code true}
     */
    @Override
    public boolean isEvent() {
        return true;
    }

    /**
     * Returns a string representation of this event card.
     *
     * @return a string containing the event type and base card info
     */
    public String toString(){
        return " {Event:" + eventType + ", " +super.toString();
    }
}

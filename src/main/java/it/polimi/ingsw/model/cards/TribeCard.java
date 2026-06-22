package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.enums.Age;

/**
 * Abstract base class for all tribe-row cards.
 *
 * <p>Tribe cards are placed in the tribe rows of the board and can be picked
 * by players on their turn. This class is the common supertype for both
 * {@link CharacterCard} and {@link EventCard}.</p>
 */
public abstract class TribeCard extends Card {

    /**
     * Creates a new tribe card.
     *
     * @param cardID  the unique identifier for this card
     * @param cardAge the era this card belongs to
     */
    public TribeCard(int cardID, Age cardAge) {
        super(cardID, cardAge);
    }

    /**
     * Returns whether this card is a character card.
     *
     * @return {@code false} by default; overridden by {@link CharacterCard}
     */
    public boolean isCharacter(){
        return false;
    }

    /**
     * Returns whether this card is an event card.
     *
     * @return {@code false} by default; overridden by {@link EventCard}
     */
    public boolean isEvent(){
        return false;
    }

    /**
     * Returns whether this card is available for games with the given number of players.
     *
     * @param numberOfPlayers the total number of players in the game
     * @return {@code true} by default; overridden by {@link CharacterCard}
     */
    public boolean isAvailableForPlayers(int numberOfPlayers) {
        return true;
    }

    /**
     * Returns {@code false} since tribe cards are not building cards.
     *
     * @return {@code false}
     */
    @Override
    public boolean isBuilding() {
        return false;
    }

    /**
     * Returns {@code true} since this is a tribe card.
     *
     * @return {@code true}
     */
    @Override
    public boolean isTribe() {
        return true;
    }

    /**
     * Returns a string representation of this tribe card.
     *
     * @return the base card string representation
     */
    public String toString(){
        return super.toString();
    }


}

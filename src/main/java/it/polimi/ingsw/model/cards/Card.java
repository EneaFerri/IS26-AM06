package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.player.Player;

/**
 * Abstract base class for all cards in the game.
 *
 * <p>Each card has a unique ID, belongs to a specific {@link Age}, and tracks
 * whether it has already been drawn from the deck.</p>
 */
public abstract class Card {

    private final int cardID;
    private final Age cardAge;
    private boolean drawed = false;

    /**
     * Creates a new card with the given ID and age.
     *
     * @param cardID  the unique identifier for this card
     * @param cardAge the era this card belongs to
     */
    public Card(int cardID, Age cardAge){
        this.cardID = cardID;
        this.cardAge = cardAge;
    }

    /**
     * Returns the unique identifier of this card.
     *
     * @return the card ID
     */
    public int getID(){
        return cardID;
    }

    /**
     * Returns the era this card belongs to.
     *
     * @return the card's {@link Age}
     */
    public Age getAge() {
        return cardAge;
    }

    /**
     * Returns whether this card has already been drawn from the deck.
     *
     * @return {@code true} if the card has been drawn
     */
    public boolean isDrawed() {
        return drawed;
    }

    /**
     * Marks this card as drawn so it cannot be selected again.
     */
    public void markAsDrawed() {
        this.drawed = true;
    }

    /**
     * Applies the card's effect when picked by a player.
     *
     * @param player the player who picked the card
     * @param game   the current game instance
     */
    public abstract void pick(Player player, Game game);

    /**
     * Returns whether this card is a building card.
     *
     * @return {@code true} if this is a {@link BuildingCard}
     */
    public boolean isBuilding() {
        return false;
    }

    /**
     * Returns whether this card is a tribe card.
     *
     * @return {@code true} if this is a {@link TribeCard}
     */
    public boolean isTribe() {
        return false;
    }

    /**
     * Returns whether this card is an event card.
     *
     * @return {@code true} if this is an {@link EventCard}
     */
    public boolean isEvent() {return false;}

    /**
     * Returns a string representation of this card showing ID and age.
     *
     * @return a string containing the card ID and age
     */
    public String toString(){
        return "CardId: " + cardID + ", Age: " + cardAge;
    }

    /**
     * Returns a human-readable label for the card's era.
     *
     * @return the era label (e.g., "Era I", "Era II")
     */
    protected String ageLabel() {
        return switch (cardAge) {
            case Era_I      -> "Era I";
            case Era_II     -> "Era II";
            case Era_III    -> "Era III";
            case Last_Event -> "Era Finale";
        };
    }

    /**
     * Returns a formatted display string for the card, suitable for UI rendering.
     *
     * @return the display string
     */
    public String toDisplayString() {
        return ageLabel();
    }


}

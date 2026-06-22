package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.InventionType;
import it.polimi.ingsw.model.player.Player;

/**
 * An Inventor character card.
 *
 * <p>Each Inventor card carries a specific invention icon. When two Inventor cards
 * with the same invention type are added to a player's collection, the player
 * receives a food bonus. Buildings can also react to Inventor pairs.</p>
 */
public class Inventor extends CharacterCard {

    private final InventionType invention;

    /**
     * Creates a new Inventor card.
     *
     * @param cardID    the unique identifier for this card
     * @param cardAge   the era this card belongs to
     * @param tag       the minimum number of players for this card to be available
     * @param invention the invention type icon this Inventor carries
     */
    public Inventor(int cardID, Age cardAge, int tag, InventionType invention) {
        super(cardID, cardAge, tag, CharacterType.INVENTOR);
        this.invention = invention;
    }

    /**
     * Returns the invention type of this Inventor card.
     *
     * @return the {@link InventionType}
     */
    public InventionType getInvention() {
        return invention;
    }

    /**
     * Triggers the invention-pair check on the player when this card is added to their collection.
     *
     * @param player the player who received this card
     */
    @Override
    public void onAddedToPlayer(Player player) {
        player.inventorsCountAndCheck(this);
    }

    /**
     * Returns whether this card carries the specified invention type.
     *
     * @param inventionType the invention type to check
     * @return {@code true} if this card's invention matches the given type
     */
    @Override
    public boolean hasInvention(InventionType inventionType) {
        return getInvention().equals(inventionType);
    }

    /**
     * Returns a formatted display string for this Inventor card.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        return "Inventore (" + ageLabel() + ")  [icona: " + invention.name() + "]";
    }

    /**
     * Returns a string representation of this Inventor card.
     *
     * @return a string containing the character info and invention type
     */
    public String toString(){
        return super.toString() + "invention: " + invention + "} ";
    }
}

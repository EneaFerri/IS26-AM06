package it.polimi.ingsw.model.cards.Characters;

import it.polimi.ingsw.model.cards.CharacterCard;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.player.Player;

/**
 * A Hunter character card.
 *
 * <p>Hunters contribute to the Hunt event, granting food and prestige per Hunter.
 * Some Hunter cards carry a nugget icon; when added to a player's collection,
 * these immediately grant food equal to the player's current number of Hunters.</p>
 */
public class Hunter extends CharacterCard {

    // 0 = no nugget icon, 1 = nugget icon (grants food on pickup)
    private final int nuggets;

    /**
     * Creates a new Hunter card.
     *
     * @param cardID  the unique identifier for this card
     * @param cardAge the era this card belongs to
     * @param tag     the minimum number of players for this card to be available
     * @param nuggets {@code 1} if this Hunter has a nugget icon (grants immediate food), {@code 0} otherwise
     */
    public Hunter(int cardID, Age cardAge, int tag, int nuggets) {
        super(cardID, cardAge, tag, CharacterType.HUNTER);
        this.nuggets = nuggets;
    }

    /**
     * Returns the nugget flag for this Hunter card.
     *
     * @return {@code 1} if this card has a nugget icon, {@code 0} otherwise
     */
    public int getNuggets() {
        return nuggets;
    }

    /**
     * Grants immediate food equal to the player's current Hunter count if this card has a nugget icon.
     *
     * @param player the player who received this card
     */
    @Override
    public void onAddedToPlayer(Player player) {
        if (getNuggets() > 0) {
            player.addFood(player.getNumHunters());
        }
    }

    /**
     * Returns a formatted display string for this Hunter card.
     *
     * @return the display string
     */
    @Override
    public String toDisplayString() {
        return "Cacciatore" + (nuggets > 0 ? " [+Cibo]" : "") + " (" + ageLabel() + ")";
    }

    /**
     * Returns a string representation of this Hunter card.
     *
     * @return a string containing the character info and nugget value
     */
    public String toString(){
        return super.toString() + ", nuggets: " + nuggets + "} ";
    }
}

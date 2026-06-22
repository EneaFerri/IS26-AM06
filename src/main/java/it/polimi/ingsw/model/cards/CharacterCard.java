package it.polimi.ingsw.model.cards;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.CharacterType;
import it.polimi.ingsw.model.enums.InventionType;
import it.polimi.ingsw.model.player.Player;

/**
 * Abstract base class for all character (tribe) cards.
 *
 * <p>Character cards are drawn from the tribe deck and added to a player's
 * collection. Each card has a {@link CharacterType}, a tag value representing
 * the minimum player count required for the card to be available, and optional
 * special abilities (prestige contribution, shaman stars, inventions, etc.).</p>
 */
public abstract class CharacterCard extends TribeCard {
    // Numeric tag: 2 means available from 2 players up, 3 from 3 players, etc.
    private final int tag;

    private final CharacterType characterType;

    /**
     * Creates a new character card.
     *
     * @param cardID        the unique identifier for this card
     * @param cardAge       the era this card belongs to
     * @param tag           the minimum number of players for this card to be available
     * @param characterType the type of character this card represents
     */
    public CharacterCard(int cardID, Age cardAge, int tag, CharacterType characterType) {
        super(cardID, cardAge);
        this.tag = tag;
        this.characterType = characterType;
    }

    /**
     * Returns the minimum player count tag for this card.
     *
     * @return the tag value (e.g., 2 = available from 2 players up)
     */
    public int getTag() {
        return tag;
    }

    /**
     * Returns the character type of this card.
     *
     * @return the {@link CharacterType}
     */
    public CharacterType getCharacterType() {
        return characterType;
    }

    /**
     * Called when this card is added to a player's collection.
     * Subclasses override this to apply immediate effects.
     *
     * @param player the player who received this card
     */
    public void onAddedToPlayer(Player player) {
    }

    /**
     * Returns the number of shaman stars this card contributes for the Ritual event.
     *
     * @return the number of shaman stars (default 0)
     */
    public int getShamanStars() {
        return 0;
    }

    /**
     * Returns the prestige points this card contributes at the end of the game.
     *
     * @param player the player who owns this card
     * @return the prestige contribution (default 0)
     */
    public int getPrestigeContribution(Player player) {
        return 0;
    }

    /**
     * Returns whether this card carries the specified invention type.
     *
     * @param inventionType the invention type to check
     * @return {@code true} if this card has the given invention (default false)
     */
    public boolean hasInvention(InventionType inventionType) {
        return false;
    }

    /**
     * Registers the character card in the game when a player picks it.
     *
     * @param player the player who picked this card
     * @param game   the current game instance
     */
    @Override
    public void pick(Player player, Game game) {
        game.pickCharacterCard(player, this);
    }

    /**
     * Returns {@code true} since this is a character card.
     *
     * @return {@code true}
     */
    @Override
    public boolean isCharacter(){
        return true;
    }

    /**
     * Returns whether this card is available given the number of players in the game.
     *
     * @param numberOfPlayers the total number of players
     * @return {@code true} if the card's tag is less than or equal to the player count
     */
    @Override
    public boolean isAvailableForPlayers(int numberOfPlayers) {
        return getTag() <= numberOfPlayers;
    }

    /**
     * Returns the food cost discount this card provides when buying buildings.
     *
     * @return the discount amount (default 0)
     */
    public int getDiscountForBuildings(){return 0;}

    /**
     * Returns a string representation of this character card.
     *
     * @return a string containing the character type and base card info
     */
    public String toString(){
        return " {" + characterType + ", " + super.toString();
    }
}

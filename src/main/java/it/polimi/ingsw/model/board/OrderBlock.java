package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.player.Totem;

/**
 * Represents a single slot on a {@link TurnOrder} tile.
 *
 * <p>Each block can hold one totem and carries a food bonus/malus and a prestige malus
 * that are applied when a player places their totem here.</p>
 */
public class OrderBlock {

    private int nuggetsBonusOrMalus;
    private int prestigeMalus;
    private Totem totemOn;

    /**
     * Creates an order block with the specified modifiers and no totem.
     *
     * @param nuggetsBonusOrMalus positive value grants food; negative value costs food (or prestige if food is zero)
     * @param prestigeMalus       prestige points lost when the player has no food to pay the malus
     */
    public OrderBlock(int nuggetsBonusOrMalus, int prestigeMalus) {
        this.nuggetsBonusOrMalus = nuggetsBonusOrMalus;
        this.prestigeMalus = prestigeMalus;
        this.totemOn = null;
    }

    /**
     * Returns the food modifier for this block (positive = bonus, negative = malus).
     *
     * @return food bonus or malus value
     */
    public int getNuggetsBonusOrMalus() {
        return nuggetsBonusOrMalus;
    }

    /**
     * Returns the prestige malus applied when the player cannot pay the food cost.
     *
     * @return prestige malus value
     */
    public int getPrestigeMalus() {
        return prestigeMalus;
    }

    /**
     * Returns the totem currently occupying this block, or {@code null} if the block is free.
     *
     * @return the totem on this block, or {@code null}
     */
    public Totem getTotemOn() {
        return totemOn;
    }

    /**
     * Returns {@code true} if no totem is placed on this block.
     *
     * @return {@code true} if the block is free
     */
    public boolean isFree() {
        return totemOn == null;
    }

    /**
     * Places the given totem on this block.
     *
     * @param totem the totem to place
     */
    public void setTotem(Totem totem) {
        this.totemOn = totem;
    }

    /**
     * Removes the totem from this block, leaving it free.
     */
    public void removeTotem() {
        this.totemOn = null;
    }
}

package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.player.Totem;

import java.io.Serializable;

/**
 * it rappresents the single order block
 * contains the bonus and the malus of the order, and the totem on the space
 * turnOrder contains a list of these blocks
 */

public class OrderBlock implements Serializable {
    private static final long serialVersionUID = 1L;

    private int nuggetsBonusOrMalus;
    private int prestigeMalus;
    private Totem totemOn;

    public OrderBlock(int nuggetsBonusOrMalus, int prestigeMalus) {
        this.nuggetsBonusOrMalus = nuggetsBonusOrMalus;
        this.prestigeMalus = prestigeMalus;
        this.totemOn = null;
    }

    public int getNuggetsBonusOrMalus() {
        return nuggetsBonusOrMalus;
    }

    public int getPrestigeMalus() {
        return prestigeMalus;
    }

    public Totem getTotemOn() {
        return totemOn;
    }


    public boolean isFree() {
        return totemOn == null;
    }


    public void setTotem(Totem totem) {
        this.totemOn = totem;
    }


    public void removeTotem() {
        this.totemOn = null;
    }
}

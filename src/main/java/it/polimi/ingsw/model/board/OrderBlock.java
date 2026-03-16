package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.player.Totem;

public class OrderBlock {

    private int nuggetsBonusOrMalus;
    private int prestigeMalus;
    private Totem totemOn;

    // solito costruttore
    public OrderBlock(int nuggetsBonusOrMalus, int prestigeMalus) {
        this.nuggetsBonusOrMalus = nuggetsBonusOrMalus;
        this.prestigeMalus = prestigeMalus;
        this.totemOn = null;
    }

    // getters
    public int getNuggetsBonusOrMalus() {
        return nuggetsBonusOrMalus;
    }

    public int getPrestigeMalus() {
        return prestigeMalus;
    }

    public Totem getTotemOn() {
        return totemOn;
    }

    //mi sa che serve anche qui
    public boolean isFree() {
        return totemOn == null;
    }

    // questo serve per metterci su il totem
    public void setTotem(Totem totem) {
        this.totemOn = totem;
    }

    // rimozione potrebbe servire
    public void removeTotem() {
        this.totemOn = null;
    }
}

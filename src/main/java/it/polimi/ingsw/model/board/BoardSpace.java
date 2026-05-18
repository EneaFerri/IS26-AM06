package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.player.Totem;

public class BoardSpace {
    private final char letter;
    private final int numCardsTop;
    private final int numCardsDown;
    private final int nuggets;
    private Totem totem;

    public BoardSpace(char letter, int numCardsTop, int numCardsDown, int nuggets) {
        this.letter = letter;
        this.numCardsTop = numCardsTop;
        this.numCardsDown = numCardsDown;
        this.nuggets = nuggets;
        this.totem = null;
    }

    public boolean isFree() {
        return totem == null;
    }

    public char getLetter() {
        return letter;
    }

    public int getTopCardsNumber() {
        return numCardsTop;
    }

    public int getBottomCardsNumber() {
        return numCardsDown;
    }

    public int getFoodReward() {
        return nuggets;
    }

    public Totem getTotem() {
        return totem;
    }

    public void setTotem(Totem totem) {
        this.totem = totem;
    }

    public void removeTotem() {
        this.totem = null;
    }

    public String toString() {
        if (totem != null) {
            return " {" + letter + ", " + "Totem: " + totem.getColor() + "nTop: " + numCardsTop + " / " + "nDown: " + numCardsDown + ", nugget bonus: " + nuggets + "} ";
        }else{
            return " {" + letter + ", no totem, " +  "nTop: " + numCardsTop + " / " + "nDown: " + numCardsDown + ", nugget bonus: " + nuggets + "} ";
        }

    }
}

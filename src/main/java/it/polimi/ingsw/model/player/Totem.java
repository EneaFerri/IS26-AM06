package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.enums.TotemColor;

import java.io.Serializable;

public class Totem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final TotemColor color;
    private BoardSpace position;

    public Totem(TotemColor color) {
        this.color = color;
        this.position = null;
    }

    public TotemColor getColor() {
        return color;
    }

    public BoardSpace getPosition() {
        return position;
    }

    public void place(BoardSpace space) {
        this.position = space;
    }

    public void remove() {
        this.position = null;
    }

    public String toString() {
        return "Color: " + color;
    }
}

package it.polimi.ingsw.model.player;

import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotemTest {

    @Test
    void newTotem_hasNoPosition() {
        Totem totem = new Totem(TotemColor.RED);

        assertNull(totem.getPosition());
    }

    @Test
    void place_setsPositionCorrectly() {
        Totem totem = new Totem(TotemColor.RED);
        BoardSpace space = new BoardSpace('A', 1, 1, 0);

        totem.place(space);

        assertEquals(space, totem.getPosition());
    }

    @Test
    void remove_clearsPosition() {
        Totem totem = new Totem(TotemColor.RED);
        BoardSpace space = new BoardSpace('A', 1, 1, 0);

        totem.place(space);
        totem.remove();

        assertNull(totem.getPosition());
    }
}
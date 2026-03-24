package it.polimi.ingsw.model.Board;

import it.polimi.ingsw.model.board.BoardSpace;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardSpaceTest {

    @Test
    void newBoardSpace_isFree() {
        BoardSpace space = new BoardSpace('A', 1, 2, 3);

        assertTrue(space.isFree());
    }

    @Test
    void setTotem_makesSpaceOccupied() {
        BoardSpace space = new BoardSpace('A', 1, 2, 3);
        Totem totem = new Totem(TotemColor.RED);

        space.setTotem(totem);

        assertFalse(space.isFree());
        assertEquals(totem, space.getTotem());
    }

    @Test
    void removeTotem_freesSpace() {
        BoardSpace space = new BoardSpace('A', 1, 2, 3);
        Totem totem = new Totem(TotemColor.RED);
        space.setTotem(totem);

        space.removeTotem();

        assertTrue(space.isFree());
        assertNull(space.getTotem());
    }

    @Test
    void getters_returnConstructorValues() {
        BoardSpace space = new BoardSpace('B', 2, 1, 5);

        assertEquals('B', space.getLetter());
        assertEquals(2, space.getTopCardsNumber());
        assertEquals(1, space.getBottomCardsNumber());
        assertEquals(5, space.getFoodReward());
    }
}
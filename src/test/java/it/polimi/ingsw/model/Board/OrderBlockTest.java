package it.polimi.ingsw.model.Board;

import it.polimi.ingsw.model.board.OrderBlock;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests OrderBlock.setTotem/removeTotem occupancy tracking and the isFree flag. */
class OrderBlockTest {

    @Test
    void constructorShouldSetValuesCorrectly() {
        OrderBlock block = new OrderBlock(2, -3);
        assertEquals(2, block.getNuggetsBonusOrMalus());
        assertEquals(-3, block.getPrestigeMalus());
        assertNull(block.getTotemOn(), "All'inizio il blocco non deve avere totem.");
        assertTrue(block.isFree(), "All'inizio il blocco deve risultare libero.");
    }

    @Test
    void setAndRemoveTotemShouldUpdateBlockState() {
        OrderBlock block = new OrderBlock(0, 0);
        Totem totem = new Totem(TotemColor.RED);

        block.setTotem(totem);
        assertFalse(block.isFree(), "Il blocco non dovrebbe più essere libero.");
        assertEquals(totem, block.getTotemOn(), "Il totem sul blocco deve essere quello appena impostato.");

        block.removeTotem();
        assertTrue(block.isFree(), "Il blocco deve tornare libero dopo la rimozione.");
        assertNull(block.getTotemOn(), "Il riferimento al totem deve essere null dopo la rimozione.");
    }
}

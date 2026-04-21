package it.polimi.ingsw.model.Board;

import it.polimi.ingsw.model.board.OrderBlock;
import it.polimi.ingsw.model.board.TurnOrder;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurnOrderTest {


    @Test
    void placeTotemFirstFreeShouldDistributeBonusAndMalus() throws Exception {
        TurnOrder turnOrder = new TurnOrder(1);

        // Creiamo due blocchi finti: uno occupato e uno libero con cibo bonus e nessun malus
        OrderBlock occupiedBlock = new OrderBlock(0, 0);
        occupiedBlock.setTotem(new Totem(TotemColor.BLUE));

        OrderBlock freeBlock = new OrderBlock(2, -1); // +2 cibo, -1 prestigio (o paga 1 cibo)

        List<OrderBlock> blocks = new ArrayList<>();
        blocks.add(occupiedBlock);
        blocks.add(freeBlock);

        // Usiamo la Reflection per inserire la lista in TurnOrder (visto che manca un metodo addOrderBlock)
        Field field = TurnOrder.class.getDeclaredField("orderBlocks");
        field.setAccessible(true);
        field.set(turnOrder, blocks);

        // Prepariamo un giocatore finto
        final int[] foodChanges = {0};
        final int[] prestigeChanges = {0};

        Player fakePlayer = new Player("Mario", new Totem(TotemColor.RED)) {
            @Override public void addFood(int amount) { foodChanges[0] += amount; }
            @Override public int getFood() { return foodChanges[0]; } // serve per simulare il check su if (player.getFood() > 0)
            @Override public void removeFood(int amount) { foodChanges[0] -= amount; }
            @Override public void addPrestige(int amount) { prestigeChanges[0] += amount; }
            @Override public boolean hasExtraFoodOnTurnOrder() { return true; } // testiamo anche la flag del bonus!
        };

        turnOrder.placeTotemFirstFree(fakePlayer);

        // Verifica: il totem deve essere andato sul secondo blocco
        assertEquals(fakePlayer.getTotem(), freeBlock.getTotemOn());

        // Verifica cibo: +2 di base dal blocco, +1 dal bonus hasExtraFoodOnTurnOrder, -1 per pagare il malus (visto che getFood > 0) = Totale 2
        assertEquals(2, foodChanges[0], "Il calcolo del cibo non è corretto.");
        assertEquals(0, prestigeChanges[0], "Non doveva subire malus prestigio perché aveva cibo per pagare.");
    }

    @Test
    void placeTotemFirstFreeShouldThrowExceptionIfFull() throws Exception {
        TurnOrder turnOrder = new TurnOrder(1);

        OrderBlock occupiedBlock = new OrderBlock(0, 0);
        occupiedBlock.setTotem(new Totem(TotemColor.BLUE));

        List<OrderBlock> blocks = List.of(occupiedBlock);
        Field field = TurnOrder.class.getDeclaredField("orderBlocks");
        field.setAccessible(true);
        field.set(turnOrder, blocks);

        Player fakePlayer = new Player("Mario", new Totem(TotemColor.RED));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            turnOrder.placeTotemFirstFree(fakePlayer);
        });
        assertEquals("No free blocks on TurnOrder tile", exception.getMessage());
    }

    @Test
    void getOrderShouldReturnPlayersInCorrectSequence() throws Exception {
        TurnOrder turnOrder = new TurnOrder(1);
        Player p1 = new Player("Mario", new Totem(TotemColor.RED));
        Player p2 = new Player("Luigi", new Totem(TotemColor.BLUE));

        OrderBlock b1 = new OrderBlock(0, 0);
        b1.setTotem(p2.getTotem()); // Luigi è primo

        OrderBlock b2 = new OrderBlock(0, 0);
        b2.setTotem(p1.getTotem()); // Mario è secondo

        List<OrderBlock> blocks = List.of(b1, b2);
        Field field = TurnOrder.class.getDeclaredField("orderBlocks");
        field.setAccessible(true);
        field.set(turnOrder, blocks);

        List<Player> ordered = turnOrder.getOrder(List.of(p1, p2));

        assertEquals(2, ordered.size());
        assertEquals(p2, ordered.get(0));
        assertEquals(p1, ordered.get(1));
    }
}

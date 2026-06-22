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

/**
 * Tests TurnOrder non-getter methods:
 * - placeTotemFirstFree: positive block gives food + extra-food flag bonus; negative block deducts food.
 * - placeTotemFirstFree: throws IllegalStateException when all blocks are occupied.
 * - getOrder: returns players sorted by their block position.
 */
class TurnOrderTest {


    @Test
    void placeTotemFirstFree_positiveFoodBlock_givesFoodPlusExtraWhenFlagSet() throws Exception {
        TurnOrder turnOrder = new TurnOrder(1);

        OrderBlock occupiedBlock = new OrderBlock(0, 0);
        occupiedBlock.setTotem(new Totem(TotemColor.BLUE));
        OrderBlock freeBlock = new OrderBlock(2, 0); // +2 food bonus, no prestige malus

        List<OrderBlock> blocks = new ArrayList<>();
        blocks.add(occupiedBlock);
        blocks.add(freeBlock);

        Field field = TurnOrder.class.getDeclaredField("orderBlocks");
        field.setAccessible(true);
        field.set(turnOrder, blocks);

        final int[] foodChanges = {0};
        final int[] prestigeChanges = {0};

        Player fakePlayer = new Player("Mario", new Totem(TotemColor.RED)) {
            @Override public void addFood(int amount)    { foodChanges[0] += amount; }
            @Override public void addPrestige(int amount) { prestigeChanges[0] += amount; }
            @Override public boolean hasExtraFoodOnTurnOrder() { return true; }
        };

        turnOrder.placeTotemFirstFree(fakePlayer);

        assertEquals(fakePlayer.getTotem(), freeBlock.getTotemOn());
        // +2 from block, +1 from extra food flag = 3
        assertEquals(3, foodChanges[0], "Food should be: +2 from block + 1 from extra food flag.");
        assertEquals(0, prestigeChanges[0], "No prestige change expected for a positive food block.");
    }

    @Test
    void placeTotemFirstFree_negativeFoodBlock_playerPaysFood_whenFoodAvailable() throws Exception {
        TurnOrder turnOrder = new TurnOrder(1);

        OrderBlock freeBlock = new OrderBlock(-1, -3); // food malus: pay 1 food or lose 3 prestige

        Field field = TurnOrder.class.getDeclaredField("orderBlocks");
        field.setAccessible(true);
        field.set(turnOrder, new ArrayList<>(List.of(freeBlock)));

        final int[] food = {5}; // player starts with 5 food
        final int[] prestige = {0};

        Player fakePlayer = new Player("Mario", new Totem(TotemColor.RED)) {
            @Override public void removeFood(int amount)  { food[0] -= amount; }
            @Override public int getFood()                { return food[0]; }
            @Override public void addPrestige(int amount) { prestige[0] += amount; }
        };

        turnOrder.placeTotemFirstFree(fakePlayer);

        assertEquals(fakePlayer.getTotem(), freeBlock.getTotemOn());
        assertEquals(4, food[0],    "Player should lose 1 food when food is available.");
        assertEquals(0, prestige[0], "Prestige should not change when player can pay with food.");
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

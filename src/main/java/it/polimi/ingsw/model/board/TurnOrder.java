package it.polimi.ingsw.model.board;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.model.cards.DeckConfiguration;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a turn-order tile, which contains a sequence of {@link OrderBlock} slots
 * where players place their totems to determine initiative and receive food bonuses or maluses.
 */
public class TurnOrder {

    private int tag;
    private List<OrderBlock> orderBlocks;

    /**
     * Creates an empty TurnOrder (used for persistence restoration).
     */
    public TurnOrder() {
        this.orderBlocks = new ArrayList<>();
    }

    /**
     * Creates a TurnOrder tile configured for the given player count.
     *
     * @param tag the player-count identifier used to select the correct block from JSON
     */
    public TurnOrder(int tag) {
        this.tag = tag;
        configureOrderBlocks(tag);
    }

    private void configureOrderBlocks(int tag) {

        ObjectMapper mapper = new ObjectMapper();
        try{
            JsonNode root = mapper.readTree(
                    getClass().getResourceAsStream("/turnOrder.json")
            );

            // retrieve the correct block by player count
            JsonNode blockNode = root.get("block" + tag);
            if (blockNode == null || !blockNode.isArray()) {
                throw new RuntimeException("Invalid block: block" + tag);
            }

            // creates only the block matching the given player count
            orderBlocks = BoardConfiguration.createTurnOrder(blockNode);


        }catch(IOException e){
            throw new RuntimeException("error loading from JSON", e);
        }
    }


    /**
     * Returns the player-count tag that identifies this turn-order tile configuration.
     *
     * @return the tag value
     */
    public int getTag() {
        return tag;
    }

    /**
     * Returns the list of order blocks that make up this tile.
     *
     * @return list of {@link OrderBlock} slots
     */
    public List<OrderBlock> getOrderBlocks() {
        return orderBlocks;
    }

    /**
     * Returns the players whose totems are on this tile, in slot order.
     *
     * @param allPlayers all players in the game
     * @return players ordered by their slot position on this tile
     */
    public List<Player> getOrder(List<Player> allPlayers) {
        List<Player> playersOrder = new ArrayList<>();
        for (OrderBlock block : orderBlocks) {
            if (!block.isFree()) {
                for (Player player : allPlayers) {
                    if (player.getTotem() == block.getTotemOn()) {
                        playersOrder.add(player);
                        break;
                    }
                }
            }
        }
        return playersOrder;
    }

    /**
     * Places the player's totem in the first free slot and applies the corresponding food bonus or malus.
     *
     * @param player the player placing their totem
     * @throws IllegalStateException if no free slots are available
     */
    public void placeTotemFirstFree(Player player) {
        for (OrderBlock block : orderBlocks) {
            if (block.isFree()) {
                block.setTotem(player.getTotem());
                player.getTotem().remove();

                int FoodbonusOrMalus = block.getNuggetsBonusOrMalus();
                int malus = block.getPrestigeMalus();

                if (FoodbonusOrMalus > 0) {
                    player.addFood(FoodbonusOrMalus);
                    if (player.hasExtraFoodOnTurnOrder()) {
                        player.addFood(1);
                    }
                }

                if(FoodbonusOrMalus < 0){
                    if(player.getFood()>0){
                        player.removeFood(1);
                    } else {
                        player.addPrestige(malus);
                    }
                }


                return;
            }
        }
        throw new IllegalStateException("No free blocks on TurnOrder tile");
    }

    /**
     * Places the player's totem in the first free slot without applying any bonus or malus
     * (used during the first round when no food modifiers are triggered).
     *
     * @param player the player placing their totem
     * @throws IllegalStateException if no free slots are available
     */
    public void placeTotemFirstFreeFirstR(Player player) {
        for (OrderBlock block : orderBlocks) {
            if (block.isFree()) {
                block.setTotem(player.getTotem());
                player.getTotem().remove();
                return;
            }
        }
        throw new IllegalStateException("No free blocks on TurnOrder tile");
    }

    /**
     * Removes the given totem from its slot on this tile.
     *
     * @param t the totem to remove
     */
    public void clearBlock(Totem t) {
        for (OrderBlock block : orderBlocks) {
            if (block.getTotemOn() == t) {
                block.getTotemOn().remove();
                block.removeTotem();
            }
        }

    }

}

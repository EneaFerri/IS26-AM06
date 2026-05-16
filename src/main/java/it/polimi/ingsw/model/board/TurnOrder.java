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
 * it represents the turn order card.
 * contains the order of the players in the game.
 * contains a list of OrderBlock
 */

public class TurnOrder {

    private int tag;
    private List<OrderBlock> orderBlocks;


    public TurnOrder(int tag) {
        this.tag = tag;
        configureOrderBlocks(tag);
    }

    //configurations with JSON file
    private void configureOrderBlocks(int tag) {

        ObjectMapper mapper = new ObjectMapper();
        try{
            JsonNode root = mapper.readTree(
                    getClass().getResourceAsStream("/turnOrder.json")
            );


            JsonNode blockNode = root.get("block" + tag); //correct block resarch
            if (blockNode == null || !blockNode.isArray()) {
                throw new RuntimeException("Blocco non valido: block" + tag);
            }

            orderBlocks = BoardConfiguration.createTurnOrder(blockNode); //create based on correct block


        }catch(IOException e){
            throw new RuntimeException("errore caricamento da JSON", e);
        }
    }


    public int getTag() {
        return tag;
    }

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

    //only for the first round
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


    public void clearBlock(Totem t) {
        for (OrderBlock block : orderBlocks) {
            if (block.getTotemOn() == t) {
                block.getTotemOn().remove();
                block.removeTotem();
            }
        }

    }

}

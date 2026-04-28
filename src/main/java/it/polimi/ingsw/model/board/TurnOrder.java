package it.polimi.ingsw.model.board;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.model.cards.DeckConfiguration;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TurnOrder {

    private int tag;
    private List<OrderBlock> orderBlocks;

    //costruttore senza tag
    public TurnOrder() {
        this.orderBlocks = new ArrayList<>();
    }

    //costruttore con tag
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


            JsonNode blockNode = root.get("block" + tag); //qui controllo il blocco corretto
            if (blockNode == null || !blockNode.isArray()) {
                throw new RuntimeException("Blocco non valido: block" + tag);
            }

            orderBlocks = BoardConfiguration.createTurnOrder(blockNode); //crea solo blocco in base a numero player


        }catch(IOException e){
            throw new RuntimeException("errore caricamento da JSON", e);
        }
    }

    // getters
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

    //TODO: SERVE? 0 USAGE E 0 CODICE LOL
    public void updateOrderFromBoard(List<BoardSpace> spaces, List<Player> players) {
    }

    //L'ho aggiunto ragas perchè non c'era un metodo che ci portasse (alla fine del turno)
    // i totem sulla carta delle offerte nell'ordine corretto
    public void placeTotemFirstFree(Player player) {
        for (OrderBlock block : orderBlocks) {
            if (block.isFree()) {
                block.setTotem(player.getTotem());
                player.getTotem().remove(); // totem non è più sull'offerta

                int bonus = block.getNuggetsBonusOrMalus();
                if (bonus > 0) {
                    player.addFood(bonus);
                    // altra condizione per edificio che da +1 cibo se si è su una casella che da cibo
                    if (player.hasExtraFoodOnTurnOrder()) {
                        player.addFood(1);
                    }
                }
                int malus = block.getPrestigeMalus();
                if (malus != 0) {
                    if (player.getFood() > 0) player.removeFood(1);
                    else player.addPrestige(malus);
                }
                return;
            }
        }
        throw new IllegalStateException("No free blocks on TurnOrder tile");
    }

    public void placeTotemFirstFreeFirstR(Player player) {
        for (OrderBlock block : orderBlocks) {
            if (block.isFree()) {
                block.setTotem(player.getTotem());
                player.getTotem().remove(); // totem non è più sull'offerta
                return;
            }
        }
        throw new IllegalStateException("No free blocks on TurnOrder tile");
    }



    //TODO: SERVE? 0 USAGE
    public void clearAll() {
        for (OrderBlock block : orderBlocks) {
            if (!block.isFree()) {
                block.getTotemOn().remove();
                block.removeTotem();
            }
        }
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

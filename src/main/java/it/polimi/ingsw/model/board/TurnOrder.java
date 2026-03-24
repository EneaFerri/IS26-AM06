package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.player.Player;

import java.util.ArrayList;
import java.util.List;

public class TurnOrder {

    private int tag;
    private List<OrderBlock> orderBlocks;

    //costruttore
    public TurnOrder(int tag) {
        this.tag = tag;
        this.orderBlocks = new ArrayList<>();
    }

    // getters
    public int getTag() {
        return tag;
    }

    public List<Player> getOrder() {
        List<Player> playersOrder = new ArrayList<>();
        for (OrderBlock block : orderBlocks) {
            if (!block.isFree() && block.getTotemOn() != null) {
                // TODO: ho buttato giù una bozza ma ho sonno ora
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
                if (bonus > 0) player.addFood(bonus);
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

    //TODO: SERVE? 0 USAGE
    public void clearAll() {
        for (OrderBlock block : orderBlocks) {
            if (!block.isFree()) {
                block.getTotemOn().remove();
                block.removeTotem();
            }
        }
    }

}

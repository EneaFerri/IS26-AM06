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

    public void saveStartRoundOrder(List<Player> players) {
    }

    // updates order according to totem positions on the board
    public void updateOrderFromBoard(List<BoardSpace> spaces, List<Player> players) {
    }

    // resets order using the left‑to‑right order of the board
    public void resetToLeft(List<Player> playersFromBoardLeftToRight) {
    }

}

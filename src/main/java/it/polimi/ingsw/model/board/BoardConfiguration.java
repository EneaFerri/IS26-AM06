package it.polimi.ingsw.model.board;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class BoardConfiguration {
    //collegamento JSON FILES: boardSpaces, turnOrder <--> CLASSE BOARD E TURNORDER

    public static BoardSpace createBoardSpace(JsonNode node) {
        Character letter = node.get("letter").asText().charAt(0);
        int nTop = node.get("numCardTop").asInt();
        int nBottom = node.get("numCardDown").asInt();
        int nuggets = node.get("nuggets").asInt();

        return new BoardSpace(letter, nTop, nBottom, nuggets);
    }

    public static List<OrderBlock> createTurnOrder(JsonNode blockNode) {

        List<OrderBlock> orderBlocks = new ArrayList<>();

        for(JsonNode elem : blockNode){
            int nuggets = elem.get("nuggets").asInt();
            int prestigeMalus = elem.get("prestigeMalus").asInt();
            OrderBlock ob = new OrderBlock(nuggets, prestigeMalus);
            orderBlocks.add(ob);
        }

        return orderBlocks;
    }
}

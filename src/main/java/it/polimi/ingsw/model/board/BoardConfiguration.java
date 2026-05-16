package it.polimi.ingsw.model.board;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Board configuration.
 * Contains the initial setup of the board.
 * link JSON FILES: boardSpaces, turnOrder <--> class Board, TurnOrder
 */

public abstract class BoardConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;


    public static BoardSpace createBoardSpace(JsonNode node) {
        Character letter = node.get("letter").asText().charAt(0);
        int nTop = node.get("numCardTop").asInt();
        int nBottom = node.get("numCardDown").asInt();
        int nuggets = node.get("nuggets").asInt();

        return new BoardSpace(letter, nTop, nBottom, nuggets);
    }

    public static List<OrderBlock> createTurnOrder(JsonNode blockNode) {
        if (blockNode == null || !blockNode.isArray()) {
            throw new IllegalArgumentException("Invalid turn order JSON: expected an array of order blocks");
        }

        List<OrderBlock> orderBlocks = new ArrayList<>();

        for (JsonNode elem : blockNode) {
            JsonNode nuggetsNode = elem.get("nuggets");
            JsonNode prestigeMalusNode = elem.get("prestigeMalus");

            if (nuggetsNode == null || prestigeMalusNode == null) {
                throw new IllegalArgumentException(
                        "Invalid turn order JSON: each block must contain 'nuggets' and 'prestigeMalus'"
                );
            }

            int nuggets = nuggetsNode.asInt();
            int prestigeMalus = prestigeMalusNode.asInt();
            OrderBlock ob = new OrderBlock(nuggets, prestigeMalus);
            orderBlocks.add(ob);
        }

        return orderBlocks;
    }
}

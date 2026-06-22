package it.polimi.ingsw.model.board;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for constructing board objects from JSON configuration nodes.
 *
 * <p>All methods are static; this class is not intended to be instantiated.</p>
 */
public abstract class BoardConfiguration {

    /**
     * Creates a {@link BoardSpace} from the given JSON node.
     *
     * @param node the JSON object containing {@code letter}, {@code numCardTop},
     *             {@code numCardDown}, and {@code nuggets} fields
     * @return a configured {@link BoardSpace} instance
     */
    public static BoardSpace createBoardSpace(JsonNode node) {
        Character letter = node.get("letter").asText().charAt(0);
        int nTop = node.get("numCardTop").asInt();
        int nBottom = node.get("numCardDown").asInt();
        int nuggets = node.get("nuggets").asInt();

        return new BoardSpace(letter, nTop, nBottom, nuggets);
    }

    /**
     * Creates the list of {@link OrderBlock} slots for a turn-order tile from the given JSON array node.
     *
     * @param blockNode a JSON array where each element contains {@code nuggets} and {@code prestigeMalus} fields
     * @return list of configured {@link OrderBlock} instances
     * @throws IllegalArgumentException if the node is null, not an array, or any element is missing required fields
     */
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

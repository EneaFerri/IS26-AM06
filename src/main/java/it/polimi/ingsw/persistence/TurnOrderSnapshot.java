package it.polimi.ingsw.persistence;

import java.util.List;

/**
 * JSON-serializable snapshot of TurnOrder.
 * {@code tag} identifies the configuration block (numberOfPlayers - 1).
 * {@code blockTotemColors} has one entry per OrderBlock;
 * {@code null} means the block is free (no totem placed).
 */
public record TurnOrderSnapshot(
        int tag,
        List<String> blockTotemColors   // TotemColor.name() or null
) {}

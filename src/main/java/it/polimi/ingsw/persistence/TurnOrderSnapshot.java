package it.polimi.ingsw.persistence;

import java.util.List;

/**
 * Snapshot JSON-serializzabile del TurnOrder.
 * tag identifica il blocco di configurazione (numberOfPlayers - 1).
 * blockTotemColors ha un elemento per ogni OrderBlock;
 * null indica che il block è libero (nessun totem posizionato).
 */
public record TurnOrderSnapshot(
        int tag,
        List<String> blockTotemColors   // TotemColor.name() oppure null
) {}

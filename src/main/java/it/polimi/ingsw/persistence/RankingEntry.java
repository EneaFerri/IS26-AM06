package it.polimi.ingsw.persistence;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Immutable DTO representing one row of the global historical leaderboard.
 * Implements Serializable so RMI can pass it directly across the wire.
 * For Socket transport it is serialized manually as a Map in SocketClientHandler.
 */
public record RankingEntry(
        int       rank,
        String    nickname,
        int       score,
        LocalDate date,
        int       numPlayers
) implements Serializable {}

package it.polimi.ingsw.database;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests DatabaseManager against an H2 in-memory database (MySQL compatibility mode).
 * The test db.properties in src/test/resources/db/ overrides the production one on the classpath.
 *
 * - saveGameResults: inserts rows retrievable via getRanking; tolerates an empty player list.
 * - getRanking: returns an empty list when the table is empty; filters by numPlayers; orders by score DESC.
 * - getPlayerRank: returns rank 1 when the table is empty; returns the correct rank among saved results.
 */
class DatabaseManagerTest {

    private static DatabaseManager db;
    private static Connection conn;

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @BeforeAll
    static void setUpClass() throws Exception {
        db = DatabaseManager.getInstance();
        Field f = DatabaseManager.class.getDeclaredField("connection");
        f.setAccessible(true);
        conn = (Connection) f.get(db);
    }

    @BeforeEach
    void truncate() throws SQLException {
        conn.createStatement().execute("DELETE FROM game_results");
    }

    /** Returns a Player whose getTotalPoints() equals the given score (prestige only, no cards). */
    private Player playerWithScore(String nick, int score) {
        Player p = new Player(nick, new Totem(TotemColor.RED));
        p.addPrestige(score);
        return p;
    }

    // =========================================================
    // Group A: saveGameResults
    // =========================================================

    @Test
    void saveGameResults_insertsRows_visibleViaRanking() throws SQLException {
        List<Player> players = List.of(playerWithScore("Alice", 10), playerWithScore("Bob", 8));
        db.saveGameResults(players, 2, TODAY);
        assertEquals(2, db.getRanking(2).size(),
                "Both saved players should appear in the ranking.");
    }

    @Test
    void saveGameResults_emptyPlayerList_doesNotThrow() {
        assertDoesNotThrow(() -> db.saveGameResults(Collections.emptyList(), 2, TODAY),
                "Saving an empty player list should not throw.");
    }

    // =========================================================
    // Group B: getRanking
    // =========================================================

    @Test
    void getRanking_emptyTable_returnsEmptyList() throws SQLException {
        assertTrue(db.getRanking(2).isEmpty(),
                "getRanking on an empty table should return an empty list.");
    }

    @Test
    void getRanking_filteredByNumPlayers_excludesOtherSizes() throws SQLException {
        db.saveGameResults(List.of(playerWithScore("Alice", 10), playerWithScore("Bob", 8)), 2, TODAY);
        db.saveGameResults(List.of(playerWithScore("Carol", 15), playerWithScore("Dave", 5),
                                   playerWithScore("Eve", 9)), 3, TODAY);

        List<RankingEntry> twoPlayerRanking = db.getRanking(2);
        assertEquals(2, twoPlayerRanking.size(),
                "getRanking(2) should only return entries from 2-player games.");
        assertTrue(twoPlayerRanking.stream().allMatch(e -> e.numPlayers() == 2),
                "All returned entries must have numPlayers == 2.");
    }

    @Test
    void getRanking_orderedByScoreDescending() throws SQLException {
        db.saveGameResults(
                List.of(playerWithScore("Alice", 20), playerWithScore("Bob", 30), playerWithScore("Carol", 10)),
                3, TODAY);

        List<RankingEntry> ranking = db.getRanking(3);
        assertEquals(3, ranking.size(), "All three entries should be returned.");
        assertEquals(30, ranking.get(0).score(), "First entry should have the highest score.");
        assertEquals(10, ranking.get(2).score(), "Last entry should have the lowest score.");
    }

    // =========================================================
    // Group C: getPlayerRank
    // =========================================================

    @Test
    void getPlayerRank_emptyTable_returnsRankOne() throws SQLException {
        assertEquals(1, db.getPlayerRank("Alice", 50, 2),
                "With no prior entries, any score should be ranked 1st.");
    }

    @Test
    void getPlayerRank_topScore_returnsRankOne() throws SQLException {
        db.saveGameResults(List.of(playerWithScore("Alice", 100), playerWithScore("Bob", 80)), 2, TODAY);
        assertEquals(1, db.getPlayerRank("Alice", 100, 2),
                "The highest score should rank 1st.");
    }

    @Test
    void getPlayerRank_lowerScore_returnsCorrectRank() throws SQLException {
        db.saveGameResults(List.of(playerWithScore("Alice", 100), playerWithScore("Bob", 80)), 2, TODAY);
        assertEquals(2, db.getPlayerRank("Bob", 80, 2),
                "A score of 80 should rank 2nd when one player scored 100.");
    }
}

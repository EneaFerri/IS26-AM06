package it.polimi.ingsw.database;

import it.polimi.ingsw.model.player.Player;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Singleton JDBC manager for the FA1 global leaderboard.
 *
 * Reads connection parameters from db.properties on the classpath.
 * All public methods are synchronized — GameController calls them from
 * the game thread, while the shutdown hook calls close() from another thread.
 *
 * Graceful degradation: every public method throws Exception upward.
 * GameController catches it and logs "[DB] Ranking unavailable" without
 * crashing the game.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() throws Exception {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/db.properties")) {
            if (in == null) throw new Exception("db.properties not found in classpath resources");
            props.load(in);
        }
        connection = DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.user"),
                props.getProperty("db.password")
        );
        createTableIfAbsent();
    }

    /**
     * Returns the singleton instance, creating it if necessary or if the
     * previous connection was closed (e.g., after a server restart mid-session).
     */
    public static synchronized DatabaseManager getInstance() throws Exception {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void createTableIfAbsent() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS game_results (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    nickname    VARCHAR(50) NOT NULL,
                    score       INT NOT NULL,
                    game_date   DATE NOT NULL,
                    num_players INT NOT NULL
                )
            """);
        }
    }

    /**
     * Inserts one row per player using a batch INSERT.
     * Called once at game end by GameController.onGameOver().
     */
    public synchronized void saveGameResults(List<Player> players,
                                             int numPlayers,
                                             LocalDate date) throws SQLException {
        String sql = "INSERT INTO game_results (nickname, score, game_date, num_players) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Player p : players) {
                ps.setString(1, p.getNickname());
                ps.setInt(2, p.getTotalPoints());
                ps.setDate(3, Date.valueOf(date));
                ps.setInt(4, numPlayers);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Returns all historical results for games of the given player count,
     * ordered by score DESC with SQL RANK() assigned sequentially.
     * Requires MySQL 8+ or PostgreSQL 9.4+.
     */
    public synchronized List<RankingEntry> getRanking(int numPlayers) throws SQLException {
        String sql = """
                SELECT nickname, score, game_date, num_players,
                       RANK() OVER (ORDER BY score DESC) AS rnk
                FROM game_results
                WHERE num_players = ?
                ORDER BY score DESC
                """;
        List<RankingEntry> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, numPlayers);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new RankingEntry(
                            rs.getInt("rnk"),
                            rs.getString("nickname"),
                            rs.getInt("score"),
                            rs.getDate("game_date").toLocalDate(),
                            rs.getInt("num_players")
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Returns the historical rank of a specific player for games of a given
     * player count. Uses COUNT to avoid a second window-function query.
     */
    public synchronized int getPlayerRank(String nickname,
                                          int score,
                                          int numPlayers) throws SQLException {
        String sql = """
                SELECT COUNT(*) + 1 AS rnk
                FROM game_results
                WHERE num_players = ? AND score > ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, numPlayers);
            ps.setInt(2, score);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("rnk") : 1;
            }
        }
    }

    /** Closes the JDBC connection. Called by CombinedServer's shutdown hook. */
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Close error: " + e.getMessage());
        }
    }
}

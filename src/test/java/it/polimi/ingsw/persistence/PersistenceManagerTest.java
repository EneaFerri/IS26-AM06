package it.polimi.ingsw.persistence;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.GameObserver;
import it.polimi.ingsw.model.enums.Age;
import it.polimi.ingsw.model.enums.GameState;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.model.player.Player;
import it.polimi.ingsw.model.player.Totem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests PersistenceManager methods using a real 2-player game started via startGame():
 * - save: creates a JSON file on disk at saves/game_{id}.json.
 * - delete: removes an existing save file; no-op when the file does not exist.
 * - loadAll: returns the saved snapshot identified by game ID.
 * - restore (round-trip): verifies that player nicknames, game state, current age, board space
 *   count, and modified player food are all preserved after save → restore.
 * - Bot nicknames: the snapshot records the provided bot nickname list.
 */
class PersistenceManagerTest {

    private static final int TEST_ID = 99998;
    private static final PersistenceManager pm = PersistenceManager.getInstance();

    private static final GameObserver NOOP = new GameObserver() {
        @Override public void onPlayerJoined(String n) {}
        @Override public void onPlayerError(String m) {}
        @Override public void onGameStarted() {}
        @Override public void onTurnStarted(String n, GameState s) {}
        @Override public void onTotemPlaced(String n, String s) {}
        @Override public void onInvalidAction(String n, String m) {}
        @Override public void onCardTaken(String n, String c) {}
        @Override public void onPlayerUpdated(String n) {}
        @Override public void onTurnOrderUpdated(List<String> l) {}
        @Override public void onEventResolved(String e, String d) {}
        @Override public void onBoardUpdated() {}
        @Override public void onNewEraStarted(Age a) {}
        @Override public void onGameOver() {}
    };

    private Game buildStartedGame() {
        Game game = new Game(TEST_ID);
        game.addPlayer(new Player("Alice", new Totem(TotemColor.RED)));
        game.addPlayer(new Player("Bob",   new Totem(TotemColor.BLUE)));
        game.addObserver(NOOP);
        game.startGame();
        return game;
    }

    @AfterEach
    void tearDown() {
        pm.delete(TEST_ID);
    }

    // =========================================================
    // Group A: save
    // =========================================================

    @Test
    void save_createsFileOnDisk() {
        Game game = buildStartedGame();
        pm.save(game, Collections.emptyList());
        assertTrue(new File("saves/game_" + TEST_ID + ".json").exists(),
                "Save file should exist after save().");
    }

    // =========================================================
    // Group B: delete
    // =========================================================

    @Test
    void delete_removesExistingFile() {
        Game game = buildStartedGame();
        pm.save(game, Collections.emptyList());
        pm.delete(TEST_ID);
        assertFalse(new File("saves/game_" + TEST_ID + ".json").exists(),
                "Save file should not exist after delete().");
    }

    @Test
    void delete_nonExistentFile_doesNotThrow() {
        assertDoesNotThrow(() -> pm.delete(TEST_ID));
    }

    // =========================================================
    // Group C: loadAll
    // =========================================================

    @Test
    void loadAll_afterSave_containsSnapshotWithCorrectGameId() {
        Game game = buildStartedGame();
        pm.save(game, Collections.emptyList());
        List<GameSnapshot> snapshots = pm.loadAll();
        assertTrue(snapshots.stream().anyMatch(s -> s.gameId() == TEST_ID),
                "loadAll() should return a snapshot with gameId " + TEST_ID);
    }

    // =========================================================
    // Group D: restore round-trip
    // =========================================================

    private GameSnapshot savedSnapshot() {
        return pm.loadAll().stream()
                .filter(s -> s.gameId() == TEST_ID)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Snapshot not found after save"));
    }

    @Test
    void restore_preservesPlayerNicknames() {
        pm.save(buildStartedGame(), Collections.emptyList());
        Game restored = pm.restore(savedSnapshot());
        List<String> nicks = restored.getPlayers().stream().map(Player::getNickname).toList();
        assertTrue(nicks.contains("Alice"), "Restored game must contain player Alice.");
        assertTrue(nicks.contains("Bob"),   "Restored game must contain player Bob.");
    }

    @Test
    void restore_preservesGameState() {
        Game game = buildStartedGame();
        GameState expected = game.getStatus();
        pm.save(game, Collections.emptyList());
        assertEquals(expected, pm.restore(savedSnapshot()).getStatus(),
                "Restored game state must match the state at save time.");
    }

    @Test
    void restore_preservesCurrentAge() {
        pm.save(buildStartedGame(), Collections.emptyList());
        assertEquals(Age.Era_I, pm.restore(savedSnapshot()).getCurrentAge(),
                "Restored age should be Era_I (set at game construction).");
    }

    @Test
    void restore_preservesBoardSpaceCount() {
        pm.save(buildStartedGame(), Collections.emptyList());
        int expected = 4; // 2-player game removes A, D, G from the 7-space board
        assertEquals(expected, pm.restore(savedSnapshot()).getBoard().getOfferField().size(),
                "Restored board should have 4 spaces for a 2-player game.");
    }

    @Test
    void restore_preservesModifiedPlayerFood() {
        Game game = buildStartedGame();
        Player alice = game.getPlayers().stream()
                .filter(p -> p.getNickname().equals("Alice"))
                .findFirst().orElseThrow();
        alice.addFood(7);
        int expectedFood = alice.getFood();

        pm.save(game, Collections.emptyList());
        Game restored = pm.restore(savedSnapshot());
        Player restoredAlice = restored.getPlayers().stream()
                .filter(p -> p.getNickname().equals("Alice"))
                .findFirst().orElseThrow();

        assertEquals(expectedFood, restoredAlice.getFood(),
                "Restored player food must match the value at save time.");
    }

    // =========================================================
    // Group E: bot nicknames
    // =========================================================

    @Test
    void save_withBotNicknames_snapshotRecordsBots() {
        pm.save(buildStartedGame(), List.of("Bob"));
        assertTrue(savedSnapshot().botNicknames().contains("Bob"),
                "Snapshot should record Bob as a bot player.");
    }
}

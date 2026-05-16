package it.polimi.ingsw.persistence;

import it.polimi.ingsw.model.Game;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles periodic serialization of Game state to disk.
 *
 * Save files are stored as saves/game_<gameId>.ser relative to the server's
 * working directory. The disk is assumed to be reliable (per FA Persistenza spec).
 *
 * On server restart, call listSavedGameIds() + load() to restore in-progress games.
 * On normal game end, call delete() to clean up the save file.
 */
public class GamePersistenceManager {

    private static final String SAVE_DIR = "saves";

    /** Saves game state to saves/game_<gameId>.ser. Fails silently on I/O error. */
    public static void save(int gameId, Game game) {
        File dir = new File(SAVE_DIR);
        dir.mkdirs();
        File file = new File(dir, "game_" + gameId + ".ser");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(game);
        } catch (IOException e) {
            System.err.println("[Persistence] Save failed for game#" + gameId + ": " + e.getMessage());
        }
    }

    /**
     * Loads a saved game from disk.
     * @return the deserialized Game, or null if the file does not exist or is corrupted.
     */
    public static Game load(int gameId) {
        File file = new File(SAVE_DIR, "game_" + gameId + ".ser");
        if (!file.exists()) return null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (Game) in.readObject();
        } catch (Exception e) {
            System.err.println("[Persistence] Load failed for game#" + gameId + ": " + e.getMessage());
            return null;
        }
    }

    /** Deletes the save file for a game that has finished normally. */
    public static void delete(int gameId) {
        File file = new File(SAVE_DIR, "game_" + gameId + ".ser");
        if (file.exists() && !file.delete()) {
            System.err.println("[Persistence] Could not delete save file for game#" + gameId);
        }
    }

    /**
     * Scans the saves/ directory and returns the IDs of all saved games.
     * Called at server startup to restore interrupted sessions.
     */
    public static List<Integer> listSavedGameIds() {
        File dir = new File(SAVE_DIR);
        List<Integer> ids = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) return ids;
        File[] files = dir.listFiles((d, name) -> name.matches("game_\\d+\\.ser"));
        if (files == null) return ids;
        for (File f : files) {
            String name = f.getName();
            try {
                int id = Integer.parseInt(name.replace("game_", "").replace(".ser", ""));
                ids.add(id);
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }
}

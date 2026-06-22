package it.polimi.ingsw.view.gui;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;

/**
 * Utility class for background music playback in the GUI.
 * Wraps a single {@link MediaPlayer} instance; all methods are static.
 */
public class MusicPlayer {

    private static MediaPlayer player;

    /**
     * Starts background music playback.
     * Does nothing if music is already playing.
     */
    public static void start() {
        if (player != null) return; // already playing

        URL resource = MusicPlayer.class.getResource("/audio/background_music.mp3");
        if (resource == null) {
            System.err.println("[MusicPlayer] File audio non trovato.");
            return;
        }

        Media media = new Media(resource.toExternalForm());
        player = new MediaPlayer(media);
        player.setCycleCount(MediaPlayer.INDEFINITE); // infinite loop
        player.setVolume(0.20); // volume 0.0 – 1.0
        player.play();
    }

    /**
     * Stops and disposes the current track.
     */
    public static void stop() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }

    /**
     * Replaces the current track with the given audio file and starts playing it.
     *
     * @param audioFileName filename (without path) of the audio resource under {@code /audio/}
     */
    public static void switchTo(String audioFileName) {
        // Stop and release the current track
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }

        URL resource = MusicPlayer.class.getResource("/audio/" + audioFileName);
        if (resource == null) {
            System.err.println("[MusicPlayer] File audio non trovato: " + audioFileName);
            return;
        }

        Media media = new Media(resource.toExternalForm());
        player = new MediaPlayer(media);
        player.setCycleCount(MediaPlayer.INDEFINITE);
        player.setVolume(0.20);
        player.play();
    }

    /**
     * Sets the playback volume.
     *
     * @param v volume level in the range {@code [0.0, 1.0]}
     */
    public static void setVolume(double v) {
        if (player != null) player.setVolume(v);
    }

    /**
     * Returns the current playback volume, or {@code 0} if no track is loaded.
     *
     * @return volume in the range {@code [0.0, 1.0]}
     */
    public static double getVolume() {
        return player != null ? player.getVolume() : 0;
    }
}

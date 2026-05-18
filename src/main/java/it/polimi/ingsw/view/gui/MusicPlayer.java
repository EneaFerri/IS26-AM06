package it.polimi.ingsw.view.gui;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;

public class MusicPlayer {

    private static MediaPlayer player;

    public static void start() {
        if (player != null) return; // già in riproduzione

        URL resource = MusicPlayer.class.getResource("/audio/background_music.mp3");
        if (resource == null) {
            System.err.println("[MusicPlayer] File audio non trovato.");
            return;
        }

        Media media = new Media(resource.toExternalForm());
        player = new MediaPlayer(media);
        player.setCycleCount(MediaPlayer.INDEFINITE); // loop infinito
        player.setVolume(0.20); // volume 0.0 – 1.0
        player.play();
    }

    public static void stop() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }

    public static void switchTo(String audioFileName) {
        // Ferma e rilascia la traccia corrente
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

    public static void setVolume(double v) {
        if (player != null) player.setVolume(v);
    }

    public static double getVolume() {
        return player != null ? player.getVolume() : 0;
    }
}
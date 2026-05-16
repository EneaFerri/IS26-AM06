package it.polimi.ingsw.network.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages a PING/PONG heartbeat for a single TCP connection.
 *
 * ── Logic ──────────────────────────────────────────────────────────────────
 * Every PING_INTERVAL_MS milliseconds the scheduler fires:
 *   1. If awaitingPong is still true (previous ping was never answered) → TIMEOUT.
 *   2. Otherwise: set awaitingPong = true and call sendPing.run().
 *
 * When the remote peer sends a PONG back, call receivedPong() to reset the flag.
 * When the remote peer sends a PING, call receivedPing(sendPong) to reply immediately.
 *
 * ── Usage (server-side handler or client) ──────────────────────────────────
 *   HeartbeatManager hb = new HeartbeatManager();
 *
 *   hb.start(
 *       () -> sendMessage(PING),          // called every interval
 *       () -> handleDisconnect("timeout") // called once on timeout
 *   );
 *
 *   // on PONG received:
 *   hb.receivedPong();
 *
 *   // on PING received:
 *   hb.receivedPing(() -> sendMessage(PONG));
 *
 *   // on clean close:
 *   hb.stop();
 */
public class PingPongManager {

    /** How often to send a PING. Must be longer than typical round-trip time. */
    private static final int PING_INTERVAL_MS = 10_000;

    private volatile ScheduledExecutorService scheduler;

    /** True while we are waiting for the PONG that follows the last PING we sent. */
    private final AtomicBoolean awaitingPong = new AtomicBoolean(false);
    private volatile boolean    running      = false;

    /**
     * Starts pinging. Must be called exactly once per connection lifetime.
     *
     * @param sendPing  called every PING_INTERVAL_MS to emit a PING
     * @param onTimeout called once when the remote peer misses a PONG
     */
    public void start(Runnable sendPing, Runnable onTimeout) {
        if (scheduler != null) scheduler.shutdownNow();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat");
            t.setDaemon(true);
            return t;
        });
        running = true;
        awaitingPong.set(false);

        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;

            if (awaitingPong.get()) {
                // The previous PING received no PONG — the peer is gone.
                running = false;
                scheduler.shutdownNow();
                onTimeout.run();
                return;
            }

            // Send the next PING and remember we're waiting for a reply.
            awaitingPong.set(true);
            try {
                sendPing.run();
            } catch (Exception e) {
                // If we can't even send the ping the connection is already broken.
                running = false;
                scheduler.shutdownNow();
                onTimeout.run();
            }

        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Call this when a PONG message is received.
     * Resets the awaiting flag so the next interval doesn't timeout.
     */
    public void receivedPong() {
        awaitingPong.set(false);
    }

    /**
     * Call this when a PING message is received.
     * Immediately invokes sendPong so the remote peer's heartbeat doesn't timeout.
     */
    public void receivedPing(Runnable sendPong) {
        try {
            sendPong.run();
        } catch (Exception e) {
            System.err.println("[HeartbeatManager] Failed to send PONG: " + e.getMessage());
        }
    }

    /**
     * Stops the heartbeat. Call on clean disconnection to avoid the onTimeout
     * callback firing after the socket has already been closed intentionally.
     */
    public void stop() {
        running = false;
        if (scheduler != null) scheduler.shutdownNow();
    }
}
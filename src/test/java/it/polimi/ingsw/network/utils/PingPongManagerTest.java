package it.polimi.ingsw.network.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests PingPongManager's callback logic without relying on the scheduled timing:
 * - receivedPong: resets the awaiting-pong flag without throwing.
 * - receivedPing: immediately invokes the provided send-pong callback.
 * - stop: terminates the scheduler cleanly whether or not start() was called.
 *
 * The 10-second scheduling interval is not exercised; only the synchronous
 * callback paths are tested here.
 */
class PingPongManagerTest {

    // =========================================================
    // Group A: receivedPong
    // =========================================================

    @Test
    void receivedPong_afterStart_doesNotThrow() {
        PingPongManager pm = new PingPongManager();
        pm.start(() -> {}, () -> {});
        assertDoesNotThrow(pm::receivedPong,
                "receivedPong() after start() should not throw.");
        pm.stop();
    }

    // =========================================================
    // Group B: receivedPing
    // =========================================================

    @Test
    void receivedPing_callsSendPongCallback() {
        PingPongManager pm = new PingPongManager();
        boolean[] called = {false};
        pm.receivedPing(() -> called[0] = true);
        assertTrue(called[0],
                "receivedPing() should invoke the sendPong callback synchronously.");
    }

    @Test
    void receivedPing_throwingCallback_doesNotPropagateException() {
        PingPongManager pm = new PingPongManager();
        assertDoesNotThrow(() -> pm.receivedPing(() -> {
            throw new RuntimeException("simulated send failure");
        }), "An exception thrown by the sendPong callback should not propagate.");
    }

    // =========================================================
    // Group C: stop
    // =========================================================

    @Test
    void stop_withoutStart_doesNotThrow() {
        PingPongManager pm = new PingPongManager();
        assertDoesNotThrow(pm::stop,
                "stop() before start() should not throw.");
    }

    @Test
    void stop_afterStart_doesNotThrow() {
        PingPongManager pm = new PingPongManager();
        pm.start(() -> {}, () -> {});
        assertDoesNotThrow(pm::stop,
                "stop() after start() should not throw.");
    }
}

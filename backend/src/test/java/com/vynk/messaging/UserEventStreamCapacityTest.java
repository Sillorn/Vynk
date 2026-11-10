package com.vynk.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * SSE capacity smoke for PN-054: many tabs per user, fan-out publish, and stream release.
 * Timings are printed as baseline; only the resource bookkeeping is asserted.
 */
class UserEventStreamCapacityTest {

    private static final int TABS_FOR_USER = 200;
    private static final int OTHER_USERS = 200;

    @Test
    void manyStreamsPerUserAreTrackedPublishedAndReleased() {
        UserEventStream streams = new UserEventStream();

        for (int tab = 0; tab < TABS_FOR_USER; tab++) {
            streams.subscribe("capgina", "session-" + tab);
        }
        for (int user = 0; user < OTHER_USERS; user++) {
            streams.subscribe("capuser" + user, "other-session-" + user);
        }
        assertThat(streams.subscriptionCount("capgina")).isEqualTo(TABS_FOR_USER);

        long publishStart = System.nanoTime();
        for (int round = 0; round < 50; round++) {
            streams.publish("capgina", "messages.updated");
        }
        long publishMillis = (System.nanoTime() - publishStart) / 1_000_000;
        // Publishing must not drop healthy streams.
        assertThat(streams.subscriptionCount("capgina")).isEqualTo(TABS_FOR_USER);

        long closeStart = System.nanoTime();
        streams.closeSession("session-0");
        long closeMicros = (System.nanoTime() - closeStart) / 1_000;
        assertThat(streams.subscriptionCount("capgina")).isEqualTo(TABS_FOR_USER - 1);

        // Logout-style release of every remaining tab for this user, leaving other users untouched.
        streams.closeUserExceptSession("capgina", null);
        assertThat(streams.subscriptionCount("capgina")).isZero();
        assertThat(streams.subscriptionCount("capuser0")).isEqualTo(1);

        System.out.println("=== PN-054 SSE capacity baseline (in-process) ===");
        System.out.println("  tabs for one user: " + TABS_FOR_USER + ", other users: " + OTHER_USERS);
        System.out.println("  50 fan-out publishes to " + TABS_FOR_USER + " streams: " + publishMillis + " ms");
        System.out.println("  closeSession across all users: " + closeMicros + " us");
    }
}

package com.vynk.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserEventStreamTest {

    private final UserEventStream streams = new UserEventStream();

    @Test
    void closeSessionOnlyEndsStreamsOpenedByThatSession() {
        streams.subscribe("gina", "session-a");
        streams.subscribe("gina", "session-a");
        streams.subscribe("gina", "session-b");
        streams.subscribe("ivy", "session-a");

        streams.closeSession("session-a");

        assertThat(streams.subscriptionCount("gina")).isEqualTo(1);
        assertThat(streams.subscriptionCount("ivy")).isZero();
    }

    @Test
    void closeUserExceptSessionKeepsOnlyTheNamedSessionAndOtherUsers() {
        streams.subscribe("hank", "keep");
        streams.subscribe("hank", "drop");
        streams.subscribe("hank", "drop");
        streams.subscribe("ivy", "drop");

        streams.closeUserExceptSession("hank", "keep");

        assertThat(streams.subscriptionCount("hank")).isEqualTo(1);
        assertThat(streams.subscriptionCount("ivy")).isEqualTo(1);

        streams.closeUserExceptSession("hank", null);
        assertThat(streams.subscriptionCount("hank")).isZero();
    }

    @Test
    void sessionDestroyedEventClosesThatSessionsStreams() {
        streams.subscribe("jon", "gone");
        streams.subscribe("jon", "alive");

        streams.onSessionDestroyed(new org.springframework.security.web.session.HttpSessionDestroyedEvent(
                new org.springframework.mock.web.MockHttpSession(null, "gone")));

        assertThat(streams.subscriptionCount("jon")).isEqualTo(1);
    }

    @Test
    void closingUnknownSessionsOrUsersIsHarmless() {
        streams.closeSession(null);
        streams.closeSession("missing");
        streams.closeUserExceptSession("nobody", "x");
        assertThat(streams.subscriptionCount("nobody")).isZero();
    }
}

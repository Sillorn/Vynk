package com.vynk.messaging;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Per-authenticated-user SSE streams. Payloads intentionally contain event identifiers only.
 *
 * <p>Every stream is bound to the HTTP session that opened it, so streams are closed when that
 * session ends (logout, expiry, invalidation) or when the account's credentials change.</p>
 */
@Component
public class UserEventStream {
    private record Subscription(String sessionId, SseEmitter emitter) {
    }

    private final ConcurrentHashMap<String, Set<Subscription>> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "vynk-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public UserEventStream() {
        heartbeat.scheduleAtFixedRate(this::sendHeartbeats, 25, 25, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe(String username, String sessionId) {
        SseEmitter emitter = new SseEmitter(0L);
        Set<Subscription> userEmitters = emitters.computeIfAbsent(username, ignored -> ConcurrentHashMap.newKeySet());
        userEmitters.add(new Subscription(sessionId, emitter));
        Runnable cleanup = () -> remove(username, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data("connected", MediaType.TEXT_PLAIN));
        } catch (IOException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    /** Notify all active tabs for a user; clients fetch authoritative state from REST. */
    public void publish(String username, String eventType) {
        Set<Subscription> userEmitters = emitters.get(username);
        if (userEmitters == null) return;
        for (Subscription subscription : userEmitters) {
            SseEmitter emitter = subscription.emitter();
            try {
                emitter.send(SseEmitter.event().name(eventType).data(eventType, MediaType.TEXT_PLAIN));
            } catch (IOException | IllegalStateException exception) {
                remove(username, emitter);
                emitter.completeWithError(exception);
            }
        }
    }

    /** Number of open streams for a user; used by tests and diagnostics. */
    int subscriptionCount(String username) {
        Set<Subscription> userEmitters = emitters.get(username);
        return userEmitters == null ? 0 : userEmitters.size();
    }

    /** Closes every stream opened by the given HTTP session. */
    public void closeSession(String sessionId) {
        if (sessionId == null) return;
        emitters.forEach((username, userEmitters) -> closeMatching(username, userEmitters,
                subscription -> sessionId.equals(subscription.sessionId())));
    }

    /** Closes all of a user's streams except those belonging to {@code keepSessionId} (may be null). */
    public void closeUserExceptSession(String username, String keepSessionId) {
        Set<Subscription> userEmitters = emitters.get(username);
        if (userEmitters == null) return;
        closeMatching(username, userEmitters,
                subscription -> keepSessionId == null || !keepSessionId.equals(subscription.sessionId()));
    }

    /** Spring Security publishes this when a servlet session is destroyed (logout, timeout, invalidate). */
    @EventListener
    public void onSessionDestroyed(SessionDestroyedEvent event) {
        closeSession(event.getId());
    }

    private void closeMatching(String username, Set<Subscription> userEmitters,
                               java.util.function.Predicate<Subscription> matches) {
        for (Subscription subscription : userEmitters) {
            if (!matches.test(subscription)) continue;
            remove(username, subscription.emitter());
            try {
                subscription.emitter().complete();
            } catch (RuntimeException ignored) {
                // Already completed or broken; nothing further to release.
            }
        }
    }

    private void sendHeartbeats() {
        emitters.forEach((username, userEmitters) -> {
            for (Subscription subscription : userEmitters) {
                SseEmitter emitter = subscription.emitter();
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException | IllegalStateException exception) {
                    remove(username, emitter);
                    emitter.completeWithError(exception);
                }
            }
        });
    }

    private void remove(String username, SseEmitter emitter) {
        Set<Subscription> userEmitters = emitters.get(username);
        if (userEmitters != null) {
            userEmitters.removeIf(subscription -> subscription.emitter() == emitter);
            if (userEmitters.isEmpty()) emitters.remove(username, userEmitters);
        }
    }
}

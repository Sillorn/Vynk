package com.vynk.messaging;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Per-authenticated-user SSE streams. Payloads intentionally contain event identifiers only. */
@Component
public class UserEventStream {
    private final ConcurrentHashMap<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "vynk-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public UserEventStream() {
        heartbeat.scheduleAtFixedRate(this::sendHeartbeats, 25, 25, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(0L);
        Set<SseEmitter> userEmitters = emitters.computeIfAbsent(username, ignored -> ConcurrentHashMap.newKeySet());
        userEmitters.add(emitter);
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
        Set<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters == null) return;
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventType).data(eventType, MediaType.TEXT_PLAIN));
            } catch (IOException | IllegalStateException exception) {
                remove(username, emitter);
                emitter.completeWithError(exception);
            }
        }
    }

    private void sendHeartbeats() {
        emitters.forEach((username, userEmitters) -> {
            for (SseEmitter emitter : userEmitters) {
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
        Set<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) emitters.remove(username, userEmitters);
        }
    }
}

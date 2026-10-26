package com.vynk.messaging.controllers;

import com.vynk.messaging.UserEventStream;
import com.vynk.shared.api.ApiPaths;
import com.vynk.shared.error.ApiException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(ApiPaths.BASE + "/events")
public class EventStreamController {
    private final UserEventStream streams;

    public EventStreamController(UserEventStream streams) { this.streams = streams; }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw ApiException.unauthenticated("Authentication required.");
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Accel-Buffering", "no")
                .body(streams.subscribe(authentication.getName()));
    }
}

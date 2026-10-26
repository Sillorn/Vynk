package com.vynk.connections.controllers;

import com.vynk.connections.controllers.ConnectionApi;
import com.vynk.connections.services.ConnectionService;
import com.vynk.shared.api.ApiPaths;
import com.vynk.shared.error.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/connections")
public class ConnectionController {
    private final ConnectionService service;

    public ConnectionController(ConnectionService service) { this.service = service; }

    @GetMapping("/requests/incoming")
    public java.util.List<ConnectionApi.Request> incoming(Authentication auth) {
        return service.incoming(requireUsername(auth));
    }

    @GetMapping("/requests/outgoing")
    public java.util.List<ConnectionApi.Request> outgoing(Authentication auth) {
        return service.outgoing(requireUsername(auth));
    }

    @PostMapping("/{username}/requests")
    public void send(@PathVariable String username, Authentication auth) {
        service.send(requireUsername(auth), username);
    }

    @PostMapping("/requests/{id}/accept")
    public void accept(@PathVariable long id, Authentication auth) {
        service.accept(requireUsername(auth), id);
    }

    @PostMapping("/requests/{id}/decline")
    public void decline(@PathVariable long id, Authentication auth) {
        service.decline(requireUsername(auth), id);
    }

    @DeleteMapping("/requests/{id}")
    public void cancel(@PathVariable long id, Authentication auth) {
        service.cancel(requireUsername(auth), id);
    }

    @DeleteMapping("/{username}")
    public void disconnect(@PathVariable String username, Authentication auth) {
        service.disconnect(requireUsername(auth), username);
    }

    private static String requireUsername(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(String.valueOf(auth.getPrincipal())))
            throw ApiException.unauthenticated("Authentication required.");
        return auth.getName();
    }
}

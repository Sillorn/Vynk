package com.vynk.connections.controllers;

import com.vynk.connections.services.ConnectionService;
import com.vynk.shared.api.ApiPaths;
import com.vynk.shared.api.PageQuery;
import com.vynk.shared.api.PageResponse;
import com.vynk.shared.error.ApiException;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/connections")
public class ConnectionController {
    private final ConnectionService service;

    public ConnectionController(ConnectionService service) { this.service = service; }

    @GetMapping
    public PageResponse<String> contacts(@Valid @ModelAttribute PageQuery page, Authentication auth) {
        return service.contacts(requireUsername(auth), page);
    }

    @GetMapping("/{username}/status")
    public boolean isConnected(@PathVariable String username, Authentication auth) {
        return service.isConnected(requireUsername(auth), username);
    }

    @GetMapping("/requests/incoming")
    public PageResponse<ConnectionApi.Request> incoming(@Valid @ModelAttribute PageQuery page, Authentication auth) {
        return service.incoming(requireUsername(auth), page);
    }

    @GetMapping("/requests/outgoing")
    public PageResponse<ConnectionApi.Request> outgoing(@Valid @ModelAttribute PageQuery page, Authentication auth) {
        return service.outgoing(requireUsername(auth), page);
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

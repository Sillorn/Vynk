package com.vynk.notifications.controllers;

import com.vynk.notifications.controllers.NotificationApi;
import com.vynk.notifications.services.NotificationService;
import com.vynk.shared.api.ApiPaths;
import com.vynk.shared.api.PageQuery;
import com.vynk.shared.api.PageResponse;
import com.vynk.shared.error.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public PageResponse<NotificationApi.Item> list(@Valid @ModelAttribute PageQuery page, Authentication auth) {
        return service.list(username(auth), page);
    }
    @PostMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable long id, Authentication auth) { service.markRead(username(auth), id); }
    private static String username(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(String.valueOf(auth.getPrincipal())))
            throw ApiException.unauthenticated("Authentication required.");
        return auth.getName();
    }
}

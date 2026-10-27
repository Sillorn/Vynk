package com.vynk.messaging.controllers;

import com.vynk.messaging.services.MessagingService;
import com.vynk.shared.api.ApiPaths;
import com.vynk.shared.api.PageQuery;
import com.vynk.shared.api.PageResponse;
import com.vynk.shared.error.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(ApiPaths.BASE + "/conversations")
public class MessagingController {
    private final MessagingService service;

    public MessagingController(MessagingService service) { this.service = service; }

    @GetMapping
    public java.util.List<MessagingApi.InboxItem> inbox(Authentication authentication) {
        return service.inbox(requireUsername(authentication));
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(@PathVariable long conversationId, Authentication authentication) {
        service.hide(requireUsername(authentication), conversationId);
    }

    @PostMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable long conversationId, Authentication authentication) {
        service.markRead(requireUsername(authentication), conversationId);
    }

    @PostMapping("/with/{username}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessagingApi.MessageResponse send(@PathVariable String username,
                                               @Valid @RequestBody MessagingApi.SendMessageRequest request,
                                               Authentication authentication) {
        return service.send(requireUsername(authentication), username, request);
    }

    @GetMapping("/{conversationId}/messages")
    public PageResponse<MessagingApi.MessageResponse> history(@PathVariable long conversationId,
                                                                @Valid @ModelAttribute PageQuery page,
                                                                Authentication authentication) {
        return service.history(requireUsername(authentication), conversationId, page);
    }

    private static String requireUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw ApiException.unauthenticated("Authentication required.");
        }
        return authentication.getName();
    }
}

package com.vynk.social.controllers;

import com.vynk.shared.api.ApiPaths;
import com.vynk.shared.api.PageQuery;
import com.vynk.shared.api.PageResponse;
import com.vynk.shared.error.ApiException;
import com.vynk.social.services.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(ApiPaths.BASE + "/posts")
public class PostController {
    private final PostService service;

    public PostController(PostService service) { this.service = service; }

    @GetMapping
    public PageResponse<PostApi.PostResponse> feed(@Valid @ModelAttribute PageQuery page) {
        return service.publicFeed(page);
    }

    @GetMapping("/connections")
    public PageResponse<PostApi.PostResponse> connectionsFeed(@Valid @ModelAttribute PageQuery page,
                                                               Authentication authentication) {
        return service.connectionsFeed(requireUsername(authentication), page);
    }

    @GetMapping("/{id}/engagement")
    public PostApi.EngagementResponse engagement(@PathVariable long id, Authentication authentication) {
        String username = authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))
                ? null : authentication.getName();
        return service.engagement(id, username);
    }

    @GetMapping("/{id}/comments")
    public PageResponse<PostApi.CommentResponse> comments(@PathVariable long id,
                                                           @Valid @ModelAttribute PageQuery page) {
        return service.comments(id, page);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public PostApi.CommentResponse addComment(@PathVariable long id,
                                               @Valid @RequestBody PostApi.CommentRequest request,
                                               Authentication authentication) {
        return service.addComment(requireUsername(authentication), id, request);
    }

    @PutMapping("/comments/{commentId}")
    public PostApi.CommentResponse updateComment(@PathVariable long commentId,
                                                 @Valid @RequestBody PostApi.CommentRequest request,
                                                 Authentication authentication) {
        return service.updateComment(requireUsername(authentication), commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable long commentId, Authentication authentication) {
        service.deleteComment(requireUsername(authentication), commentId);
    }

    @PostMapping("/{id}/likes")
    public PostApi.LikeResponse toggleLike(@PathVariable long id, Authentication authentication) {
        return service.toggleLike(requireUsername(authentication), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostApi.PostResponse create(@Valid @RequestBody PostApi.WriteRequest request,
                                       Authentication authentication) {
        return service.create(requireUsername(authentication), request);
    }

    @PutMapping("/{id}")
    public PostApi.PostResponse update(@PathVariable long id, @Valid @RequestBody PostApi.WriteRequest request,
                                       Authentication authentication) {
        return service.update(requireUsername(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, Authentication authentication) {
        service.delete(requireUsername(authentication), id);
    }

    private static String requireUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw ApiException.unauthenticated("Authentication required.");
        }
        return authentication.getName();
    }
}

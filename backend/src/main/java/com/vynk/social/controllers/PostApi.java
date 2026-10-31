package com.vynk.social.controllers;

import com.vynk.social.entities.Post;
import com.vynk.social.entities.PostComment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class PostApi {
    private PostApi() {}

    public record WriteRequest(@NotBlank @Size(max = 5000) String content,
                               @jakarta.validation.constraints.Pattern(regexp = "PUBLIC|CONNECTIONS") String audience) {}
    public record PostResponse(Long id, String username, String content, String audience, Instant createdAt, Instant updatedAt) {
        public static PostResponse from(Post post) {
            return new PostResponse(post.getId(), post.getAuthor().getUsername(), post.getContent(), post.getAudience(),
                    post.getCreatedAt(), post.getUpdatedAt());
        }
    }
    public record CommentRequest(@NotBlank @Size(max = 2000) String content) {}
    public record CommentResponse(Long id, Long postId, String username, String content,
                                  Instant createdAt, Instant updatedAt) {
        public static CommentResponse from(PostComment comment) {
            return new CommentResponse(comment.getId(), comment.getPost().getId(),
                    comment.getAuthor().getUsername(), comment.getContent(),
                    comment.getCreatedAt(), comment.getUpdatedAt());
        }
    }
    public record LikeResponse(boolean liked, long likeCount) {}
    public record EngagementResponse(boolean liked, long likeCount, long commentCount) {}
}

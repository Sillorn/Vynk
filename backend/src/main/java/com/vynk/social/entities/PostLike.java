package com.vynk.social.entities;

import com.vynk.identity.entities.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "post_like")
public class PostLike {
    @EmbeddedId private PostLikeId id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @MapsId("postId")
    @JoinColumn(name = "post_id", nullable = false) private Post post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected PostLike() {}
    public PostLike(Post post, UserAccount user) { this.id = new PostLikeId(post.getId(), user.getId()); this.post = post; this.user = user; }
}

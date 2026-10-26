package com.vynk.social.repositories;

import com.vynk.social.entities.PostLike;
import com.vynk.social.entities.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
    boolean existsById(PostLikeId id);
    long countByIdPostId(Long postId);
}

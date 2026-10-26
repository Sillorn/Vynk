package com.vynk.social.repositories;

import com.vynk.social.entities.PostComment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    Page<PostComment> findByPostIdOrderByCreatedAtAscIdAsc(Long postId, Pageable pageable);
    @Query("select c from PostComment c join fetch c.author where c.id = :id and c.author.username = :username")
    Optional<PostComment> findOwned(@Param("id") Long id, @Param("username") String username);
    long countByPostId(Long postId);
}

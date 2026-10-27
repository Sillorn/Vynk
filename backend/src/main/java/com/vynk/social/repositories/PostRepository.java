package com.vynk.social.repositories;

import com.vynk.social.entities.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("select p from Post p join fetch p.author a where a.status = com.vynk.identity.UserAccountStatus.ACTIVE order by p.createdAt desc, p.id desc")
    Page<Post> findPublicFeed(Pageable pageable);

    @Query(value = "select p.* from post p join user_account author on author.id = p.author_id where author.status = 'ACTIVE' and author.id in (select case when c.user_low_id = :userId then c.user_high_id else c.user_low_id end from accepted_connection c where c.user_low_id = :userId or c.user_high_id = :userId) order by p.created_at desc, p.id desc",
            countQuery = "select count(*) from post p join user_account author on author.id = p.author_id where author.status = 'ACTIVE' and author.id in (select case when c.user_low_id = :userId then c.user_high_id else c.user_low_id end from accepted_connection c where c.user_low_id = :userId or c.user_high_id = :userId)",
            nativeQuery = true)
    Page<Post> findConnectionsFeed(@Param("userId") Long userId, Pageable pageable);

    @Query("select p from Post p join fetch p.author a where p.id = :id and a.username = :username")
    java.util.Optional<Post> findOwned(@Param("id") Long id, @Param("username") String username);
}

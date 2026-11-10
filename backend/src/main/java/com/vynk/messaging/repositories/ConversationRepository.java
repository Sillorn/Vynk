package com.vynk.messaging.repositories;

import com.vynk.messaging.entities.Conversation;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByUserLowIdAndUserHighId(long low, long high);

    /**
     * Conversations visible to the user (not hidden by them), newest activity first. The sort is
     * applied in the query, so callers must pass an unsorted {@link Pageable}.
     */
    @Query(value = """
            select c from Conversation c
            left join ConversationMessage m on m.conversation = c
            where (c.userLowId = :userId or c.userHighId = :userId)
              and not exists (select s.conversationId from ConversationUserState s
                              where s.conversationId = c.id and s.userId = :userId and s.hidden = true)
            group by c
            order by max(m.id) desc nulls last, c.id desc
            """,
            countQuery = """
            select count(c) from Conversation c
            where (c.userLowId = :userId or c.userHighId = :userId)
              and not exists (select s.conversationId from ConversationUserState s
                              where s.conversationId = c.id and s.userId = :userId and s.hidden = true)
            """)
    Page<Conversation> findVisibleForUser(@Param("userId") long userId, Pageable pageable);
}

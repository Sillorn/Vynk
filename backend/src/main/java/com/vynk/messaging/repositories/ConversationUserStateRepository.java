package com.vynk.messaging.repositories;

import com.vynk.messaging.entities.ConversationUserState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationUserStateRepository extends JpaRepository<ConversationUserState, ConversationUserState.Key> {
    Optional<ConversationUserState> findByConversationIdAndUserId(long conversationId, long userId);

    /** Number of visible conversations the user has unread messages in. */
    @Query("""
            select count(s) from ConversationUserState s
            where s.userId = :userId and s.hidden = false
              and exists (select m.id from ConversationMessage m
                          where m.conversation.id = s.conversationId and m.senderId <> :userId
                            and (s.lastReadMessage is null or m.id > s.lastReadMessage.id))
            """)
    long countConversationsWithUnread(@Param("userId") long userId);
}

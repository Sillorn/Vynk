package com.vynk.messaging.repositories;

import com.vynk.messaging.entities.ConversationMessage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    /** Projection of unread message counts for one conversation. */
    interface UnreadCount {
        Long getConversationId();
        Long getUnread();
    }

    Page<ConversationMessage> findByConversationId(long conversationId, Pageable pageable);
    java.util.Optional<ConversationMessage> findBySenderIdAndClientRequestId(long senderId, java.util.UUID clientRequestId);
    java.util.Optional<ConversationMessage> findFirstByConversationIdOrderByIdDesc(long conversationId);

    /** Latest message of each given conversation, fetched in a single query. */
    @Query("""
            select m from ConversationMessage m
            where m.id in (select max(x.id) from ConversationMessage x
                           where x.conversation.id in :conversationIds
                           group by x.conversation.id)
            """)
    List<ConversationMessage> findLatestByConversationIds(@Param("conversationIds") Collection<Long> conversationIds);

    /** Unread messages (sent by the other participant after the user's read marker), per conversation. */
    @Query("""
            select m.conversation.id as conversationId, count(m) as unread
            from ConversationMessage m, ConversationUserState s
            where s.conversationId = m.conversation.id and s.userId = :userId
              and m.conversation.id in :conversationIds
              and m.senderId <> :userId
              and (s.lastReadMessage is null or m.id > s.lastReadMessage.id)
            group by m.conversation.id
            """)
    List<UnreadCount> countUnreadByConversation(@Param("userId") long userId,
                                                @Param("conversationIds") Collection<Long> conversationIds);
}

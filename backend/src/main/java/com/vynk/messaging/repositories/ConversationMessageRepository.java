package com.vynk.messaging.repositories;

import com.vynk.messaging.entities.ConversationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    Page<ConversationMessage> findByConversationId(long conversationId, Pageable pageable);
    java.util.Optional<ConversationMessage> findBySenderIdAndClientRequestId(long senderId, java.util.UUID clientRequestId);
    java.util.Optional<ConversationMessage> findFirstByConversationIdOrderByIdDesc(long conversationId);
    long countByConversationIdAndIdGreaterThanAndSenderIdNot(long conversationId, long messageId, long senderId);
    long countByConversationIdAndSenderIdNot(long conversationId, long senderId);
}

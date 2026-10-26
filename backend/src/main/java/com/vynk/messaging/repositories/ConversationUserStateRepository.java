package com.vynk.messaging.repositories;

import com.vynk.messaging.entities.ConversationUserState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationUserStateRepository extends JpaRepository<ConversationUserState, ConversationUserState.Key> {
    Optional<ConversationUserState> findByConversationIdAndUserId(long conversationId, long userId);
}

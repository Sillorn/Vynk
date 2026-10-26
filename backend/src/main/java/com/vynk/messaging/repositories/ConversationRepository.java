package com.vynk.messaging.repositories;

import com.vynk.messaging.entities.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByUserLowIdAndUserHighId(long low, long high);
    List<Conversation> findByUserLowIdOrUserHighId(long userLowId, long userHighId);
}

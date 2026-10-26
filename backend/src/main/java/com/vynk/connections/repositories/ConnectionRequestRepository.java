package com.vynk.connections.repositories;

import com.vynk.connections.ConnectionRequestStatus;
import com.vynk.connections.entities.ConnectionRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {
    Optional<ConnectionRequest> findBySenderUsernameIgnoreCaseAndRecipientUsernameIgnoreCase(
            String senderUsername, String recipientUsername);

    boolean existsBySenderUsernameIgnoreCaseAndRecipientUsernameIgnoreCase(
            String senderUsername, String recipientUsername);

    java.util.List<ConnectionRequest> findByRecipientUsernameIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String username, ConnectionRequestStatus status);

    java.util.List<ConnectionRequest> findBySenderUsernameIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String username, ConnectionRequestStatus status);
}

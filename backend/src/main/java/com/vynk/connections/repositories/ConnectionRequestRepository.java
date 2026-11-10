package com.vynk.connections.repositories;

import com.vynk.connections.ConnectionRequestStatus;
import com.vynk.connections.entities.ConnectionRequest;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {
    Optional<ConnectionRequest> findBySenderUsernameIgnoreCaseAndRecipientUsernameIgnoreCase(
            String senderUsername, String recipientUsername);

    boolean existsBySenderUsernameIgnoreCaseAndRecipientUsernameIgnoreCase(
            String senderUsername, String recipientUsername);

    Page<ConnectionRequest> findByRecipientUsernameIgnoreCaseAndStatusOrderByCreatedAtDescIdDesc(
            String username, ConnectionRequestStatus status, Pageable pageable);

    Page<ConnectionRequest> findBySenderUsernameIgnoreCaseAndStatusOrderByCreatedAtDescIdDesc(
            String username, ConnectionRequestStatus status, Pageable pageable);
}

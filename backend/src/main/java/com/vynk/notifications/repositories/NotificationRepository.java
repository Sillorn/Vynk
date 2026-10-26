package com.vynk.notifications.repositories;

import com.vynk.notifications.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientUsernameIgnoreCaseOrderByIdDesc(String username, Pageable pageable);
    java.util.Optional<Notification> findByIdAndRecipientUsernameIgnoreCase(long id, String username);
}

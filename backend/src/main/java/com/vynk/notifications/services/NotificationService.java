package com.vynk.notifications.services;

import com.vynk.notifications.controllers.NotificationApi;
import com.vynk.notifications.repositories.NotificationRepository;
import com.vynk.shared.api.PageQuery;
import com.vynk.shared.api.PageResponse;
import com.vynk.shared.error.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    public NotificationService(NotificationRepository notifications) { this.notifications = notifications; }

    @Transactional(readOnly = true)
    public PageResponse<NotificationApi.Item> list(String username, PageQuery query) {
        var page = notifications.findByRecipientUsernameIgnoreCaseOrderByIdDesc(username,
                PageRequest.of(query.pageNumber(), query.pageSize()));
        return PageResponse.of(page.getContent().stream().map(n -> new NotificationApi.Item(n.getId(), n.getType(),
                n.getActorUsername(), n.getCreatedAt(), n.getReadAt())).toList(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public void markRead(String username, long id) {
        var notification = notifications.findByIdAndRecipientUsernameIgnoreCase(id, username)
                .orElseThrow(() -> ApiException.notFound("Notification not found."));
        notification.markRead();
    }
}

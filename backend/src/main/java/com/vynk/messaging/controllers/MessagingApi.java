package com.vynk.messaging.controllers;

import com.vynk.messaging.entities.ConversationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class MessagingApi {
    private MessagingApi() {}

    public record SendMessageRequest(
            @NotBlank @Size(max = 5000) String body,
            @NotNull UUID clientRequestId) {}

    public record InboxItem(long conversationId, long participantId, String participantUsername,
                            MessageResponse latestMessage, long unreadCount) {}

    public record MessageResponse(long id, long conversationId, long senderId, String senderUsername,
                                  String body, Instant createdAt) {
        public static MessageResponse from(ConversationMessage message, String username) {
            return new MessageResponse(message.getId(), message.getConversation().getId(),
                    message.getSenderId(), username, message.getBody(), message.getCreatedAt());
        }
    }
}

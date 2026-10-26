package com.vynk.messaging.entities;

import com.vynk.messaging.entities.ConversationMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation_user_state")
@IdClass(ConversationUserState.Key.class)
public class ConversationUserState {
    @Id
    @Column(name = "conversation_id", nullable = false, updatable = false)
    private Long conversationId;

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private ConversationMessage lastReadMessage;

    @Column(nullable = false)
    private boolean hidden;

    protected ConversationUserState() {}

    public ConversationUserState(long conversationId, long userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    public Long getConversationId() { return conversationId; }
    public Long getUserId() { return userId; }
    public ConversationMessage getLastReadMessage() { return lastReadMessage; }
    public boolean isHidden() { return hidden; }
    public void setLastReadMessage(ConversationMessage message) { this.lastReadMessage = message; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public static class Key implements java.io.Serializable {
        private Long conversationId;
        private Long userId;

        public Key() {}
        public Key(Long conversationId, Long userId) {
            this.conversationId = conversationId;
            this.userId = userId;
        }

        @Override public boolean equals(Object other) {
            return other instanceof Key key
                && java.util.Objects.equals(conversationId, key.conversationId)
                && java.util.Objects.equals(userId, key.userId);
        }

        @Override public int hashCode() {
            return java.util.Objects.hash(conversationId, userId);
        }
    }
}

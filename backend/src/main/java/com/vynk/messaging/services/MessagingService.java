package com.vynk.messaging.services;

import com.vynk.connections.repositories.AcceptedConnectionRepository;
import com.vynk.identity.entities.UserAccount;
import com.vynk.identity.repositories.UserAccountRepository;
import com.vynk.messaging.UserEventStream;
import com.vynk.messaging.controllers.MessagingApi;
import com.vynk.messaging.entities.Conversation;
import com.vynk.messaging.entities.ConversationMessage;
import com.vynk.messaging.entities.ConversationUserState;
import com.vynk.messaging.repositories.ConversationMessageRepository;
import com.vynk.messaging.repositories.ConversationRepository;
import com.vynk.messaging.repositories.ConversationUserStateRepository;
import com.vynk.shared.api.PageQuery;
import com.vynk.shared.api.PageResponse;
import com.vynk.shared.error.ApiException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MessagingService {
    private final ConversationRepository conversations;
    private final ConversationMessageRepository messages;
    private final ConversationUserStateRepository states;
    private final AcceptedConnectionRepository connections;
    private final UserAccountRepository accounts;
    private final UserEventStream eventStream;

    public MessagingService(ConversationRepository conversations, ConversationMessageRepository messages,
                            ConversationUserStateRepository states, AcceptedConnectionRepository connections,
                            UserAccountRepository accounts, UserEventStream eventStream) {
        this.conversations = conversations;
        this.messages = messages;
        this.states = states;
        this.connections = connections;
        this.accounts = accounts;
        this.eventStream = eventStream;
    }

    @Transactional
    public MessagingApi.MessageResponse send(String actorUsername, String targetUsername,
                                             MessagingApi.SendMessageRequest request) {
        UserAccount actor = account(actorUsername);
        UserAccount target = account(targetUsername);
        if (actor.getId().equals(target.getId())) throw ApiException.badRequest("You cannot message yourself.");
        long low = Math.min(actor.getId(), target.getId());
        long high = Math.max(actor.getId(), target.getId());
        if (!connections.existsPair(low, high)) {
            throw ApiException.forbidden("Messaging requires an accepted connection.");
        }

        var prior = messages.findBySenderIdAndClientRequestId(actor.getId(), request.clientRequestId());
        if (prior.isPresent()) {
            ConversationMessage existing = prior.get();
            if (!existing.getConversation().getUserLowId().equals(low)
                    || !existing.getConversation().getUserHighId().equals(high)
                    || !existing.getBody().equals(request.body().trim())) {
                throw ApiException.conflict("Client request ID was already used for a different message.");
            }
            return MessagingApi.MessageResponse.from(existing, actor.getUsername());
        }

        Conversation conversation = conversations.findByUserLowIdAndUserHighId(low, high).orElseGet(() -> {
            Conversation created = conversations.saveAndFlush(new Conversation(low, high));
            states.save(new ConversationUserState(created.getId(), low));
            states.save(new ConversationUserState(created.getId(), high));
            return created;
        });
        ConversationMessage saved = messages.saveAndFlush(
                new ConversationMessage(conversation, actor.getId(), request.body().trim(), request.clientRequestId()));
        states.findByConversationIdAndUserId(conversation.getId(), actor.getId())
                .ifPresent(state -> state.setHidden(false));
        states.findByConversationIdAndUserId(conversation.getId(), target.getId())
                .ifPresent(state -> state.setHidden(false));
        String recipientUsername = target.getUsername();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventStream.publish(recipientUsername, "message");
            }
        });
        return MessagingApi.MessageResponse.from(saved, actor.getUsername());
    }

    @Transactional(readOnly = true)
    public java.util.List<MessagingApi.InboxItem> inbox(String actorUsername) {
        UserAccount actor = account(actorUsername);
        return conversations.findByUserLowIdOrUserHighId(actor.getId(), actor.getId()).stream()
                .filter(conversation -> !states.findByConversationIdAndUserId(conversation.getId(), actor.getId())
                        .map(ConversationUserState::isHidden).orElse(false))
                .map(conversation -> {
                    long participantId = conversation.getUserLowId() == actor.getId()
                            ? conversation.getUserHighId() : conversation.getUserLowId();
                    UserAccount participant = accounts.findById(participantId).orElseThrow();
                    var latest = messages.findFirstByConversationIdOrderByIdDesc(conversation.getId())
                            .map(message -> MessagingApi.MessageResponse.from(message,
                                    accounts.findById(message.getSenderId()).map(UserAccount::getUsername).orElse("")))
                            .orElse(null);
                    long unread = states.findByConversationIdAndUserId(conversation.getId(), actor.getId())
                            .map(state -> state.getLastReadMessage() == null
                                    ? messages.countByConversationIdAndSenderIdNot(conversation.getId(), actor.getId())
                                    : messages.countByConversationIdAndIdGreaterThanAndSenderIdNot(conversation.getId(),
                                            state.getLastReadMessage().getId(), actor.getId()))
                            .orElse(0L);
                    return new MessagingApi.InboxItem(conversation.getId(), participantId,
                            participant.getUsername(), latest, unread);
                })
                .sorted((first, second) -> Long.compare(
                        second.latestMessage() == null ? 0 : second.latestMessage().id(),
                        first.latestMessage() == null ? 0 : first.latestMessage().id()))
                .toList();
    }

    @Transactional
    public void hide(String actorUsername, long conversationId) {
        UserAccount actor = account(actorUsername);
        Conversation conversation = conversations.findById(conversationId)
                .filter(item -> item.hasParticipant(actor.getId()))
                .orElseThrow(() -> ApiException.notFound("Conversation not found."));
        ConversationUserState state = states.findByConversationIdAndUserId(conversationId, actor.getId())
                .orElseGet(() -> states.save(new ConversationUserState(conversationId, actor.getId())));
        state.setHidden(true);
    }

    @Transactional
    public void markRead(String actorUsername, long conversationId) {
        UserAccount actor = account(actorUsername);
        Conversation conversation = conversations.findById(conversationId)
                .filter(item -> item.hasParticipant(actor.getId()))
                .orElseThrow(() -> ApiException.notFound("Conversation not found."));
        var state = states.findByConversationIdAndUserId(conversation.getId(), actor.getId())
                .orElseGet(() -> states.save(new ConversationUserState(conversation.getId(), actor.getId())));
        messages.findFirstByConversationIdOrderByIdDesc(conversation.getId()).ifPresent(state::setLastReadMessage);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessagingApi.MessageResponse> history(String actorUsername, long conversationId,
                                                               PageQuery page) {
        UserAccount actor = account(actorUsername);
        Conversation conversation = conversations.findById(conversationId)
                .filter(item -> item.hasParticipant(actor.getId()))
                .orElseThrow(() -> ApiException.notFound("Conversation not found."));
        var result = messages.findByConversationId(conversation.getId(), PageRequest.of(
                page.pageNumber(), page.pageSize(), Sort.by(Sort.Direction.DESC, "id")));
        Map<Long, String> usernames = accounts.findAllById(result.getContent().stream()
                .map(ConversationMessage::getSenderId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(UserAccount::getId, UserAccount::getUsername));
        var items = result.getContent().stream().map(message -> MessagingApi.MessageResponse.from(
                message, usernames.getOrDefault(message.getSenderId(), ""))).toList();
        return PageResponse.of(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private UserAccount account(String username) {
        return accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }
}

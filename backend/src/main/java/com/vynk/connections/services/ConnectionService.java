package com.vynk.connections.services;

import com.vynk.connections.ConnectionRequestStatus;
import com.vynk.connections.controllers.ConnectionApi;
import com.vynk.connections.entities.AcceptedConnection;
import com.vynk.connections.entities.ConnectionRequest;
import com.vynk.connections.repositories.AcceptedConnectionRepository;
import com.vynk.connections.repositories.ConnectionRequestRepository;
import com.vynk.identity.entities.UserAccount;
import com.vynk.identity.repositories.UserAccountRepository;
import com.vynk.notifications.entities.Notification;
import com.vynk.notifications.repositories.NotificationRepository;
import com.vynk.messaging.UserEventStream;
import com.vynk.shared.api.PageQuery;
import com.vynk.shared.api.PageResponse;
import com.vynk.shared.error.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ConnectionService {
    private final ConnectionRequestRepository requests;
    private final AcceptedConnectionRepository accepted;
    private final UserAccountRepository accounts;
    private final NotificationRepository notifications;
    private final UserEventStream eventStream;

    public ConnectionService(ConnectionRequestRepository requests, AcceptedConnectionRepository accepted,
                             UserAccountRepository accounts, NotificationRepository notifications,
                             UserEventStream eventStream) {
        this.requests = requests;
        this.accepted = accepted;
        this.accounts = accounts;
        this.notifications = notifications;
        this.eventStream = eventStream;
    }

    @Transactional(readOnly = true)
    public PageResponse<String> contacts(String actor, PageQuery query) {
        Page<String> page = accepted.findContacts(account(actor).getId(), pageRequest(query));
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public boolean isConnected(String actor, String target) {
        UserAccount a = account(actor), b = account(target);
        return !a.getId().equals(b.getId()) && accepted.existsById(new AcceptedConnection.Key(
                Math.min(a.getId(), b.getId()), Math.max(a.getId(), b.getId())));
    }

    @Transactional(readOnly = true)
    public PageResponse<ConnectionApi.Request> incoming(String actor, PageQuery query) {
        Page<ConnectionRequest> page = requests.findByRecipientUsernameIgnoreCaseAndStatusOrderByCreatedAtDescIdDesc(
                actor, ConnectionRequestStatus.PENDING, pageRequest(query));
        return toResponse(page, r -> new ConnectionApi.Request(r.getId(), r.getSender().getUsername(), r.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ConnectionApi.Request> outgoing(String actor, PageQuery query) {
        Page<ConnectionRequest> page = requests.findBySenderUsernameIgnoreCaseAndStatusOrderByCreatedAtDescIdDesc(
                actor, ConnectionRequestStatus.PENDING, pageRequest(query));
        return toResponse(page, r -> new ConnectionApi.Request(r.getId(), r.getRecipient().getUsername(), r.getCreatedAt()));
    }

    @Transactional
    public void send(String actor, String target) {
        UserAccount from = account(actor);
        UserAccount to = account(target);
        if (from.getId().equals(to.getId())) throw ApiException.badRequest("You cannot connect to yourself.");
        long low = Math.min(from.getId(), to.getId()), high = Math.max(from.getId(), to.getId());
        if (accepted.existsById(new AcceptedConnection.Key(low, high))) throw ApiException.conflict("Already connected.");
        // A pending request from the other person must be accepted, not mirrored. Check it before
        // reviving any earlier outgoing request so both directions can never be pending at once.
        var incoming = requests.findBySenderUsernameIgnoreCaseAndRecipientUsernameIgnoreCase(target, actor);
        if (incoming.isPresent() && incoming.get().getStatus() == ConnectionRequestStatus.PENDING)
            throw ApiException.conflict("This person has already requested to connect.");
        var outgoing = requests.findBySenderUsernameIgnoreCaseAndRecipientUsernameIgnoreCase(actor, target);
        if (outgoing.isPresent()) {
            if (outgoing.get().getStatus() == ConnectionRequestStatus.PENDING)
                throw ApiException.conflict("A pending request already exists.");
            outgoing.get().setStatus(ConnectionRequestStatus.PENDING);
            return;
        }
        try {
            requests.save(new ConnectionRequest(from, to));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("A connection request already exists.");
        }
    }

    @Transactional
    public void cancel(String actor, long id) { transition(actor, id, ConnectionRequestStatus.PENDING, ConnectionRequestStatus.CANCELLED, true); }

    @Transactional
    public void decline(String actor, long id) { transition(actor, id, ConnectionRequestStatus.PENDING, ConnectionRequestStatus.DECLINED, false); }

    @Transactional
    public void accept(String actor, long id) {
        ConnectionRequest request = request(id);
        if (!request.getRecipient().getUsername().equalsIgnoreCase(actor)) throw ApiException.notFound("Connection request not found.");
        if (request.getStatus() != ConnectionRequestStatus.PENDING) throw ApiException.conflict("Request is no longer pending.");
        long low = Math.min(request.getSender().getId(), request.getRecipient().getId());
        long high = Math.max(request.getSender().getId(), request.getRecipient().getId());
        // save() on an assigned composite key would silently merge an existing row, so check first.
        if (accepted.existsById(new AcceptedConnection.Key(low, high)))
            throw ApiException.conflict("These users are already connected.");
        try {
            accepted.saveAndFlush(new AcceptedConnection(low, high));
            request.setStatus(ConnectionRequestStatus.ACCEPTED);
            notifications.save(new Notification(request.getSender(), "CONNECTION_ACCEPTED",
                    request.getRecipient().getUsername()));
            String senderUsername = request.getSender().getUsername();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventStream.publish(senderUsername, "notification");
                }
            });
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("These users are already connected.");
        }
    }

    @Transactional
    public void disconnect(String actor, String target) {
        UserAccount a = account(actor), b = account(target);
        if (a.getId().equals(b.getId())) throw ApiException.badRequest("Invalid connection.");
        long low = Math.min(a.getId(), b.getId()), high = Math.max(a.getId(), b.getId());
        if (accepted.deletePair(low, high) == 0) throw ApiException.notFound("Connection not found.");
    }

    private void transition(String actor, long id, ConnectionRequestStatus expected,
                            ConnectionRequestStatus next, boolean sender) {
        ConnectionRequest req = request(id);
        String owner = sender ? req.getSender().getUsername() : req.getRecipient().getUsername();
        if (!owner.equalsIgnoreCase(actor)) throw ApiException.notFound("Connection request not found.");
        if (req.getStatus() != expected) throw ApiException.conflict("Request is no longer pending.");
        req.setStatus(next);
    }

    private static PageRequest pageRequest(PageQuery query) {
        return PageRequest.of(query.pageNumber(), query.pageSize());
    }

    private static <S, T> PageResponse<T> toResponse(Page<S> page, java.util.function.Function<S, T> mapper) {
        return PageResponse.of(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private ConnectionRequest request(long id) {
        return requests.findById(id).orElseThrow(() -> ApiException.notFound("Connection request not found."));
    }

    private UserAccount account(String username) {
        return accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }
}

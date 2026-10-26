package com.vynk.connections.services;

import com.vynk.connections.ConnectionRequestStatus;
import com.vynk.connections.controllers.ConnectionApi;
import com.vynk.connections.entities.AcceptedConnection;
import com.vynk.connections.entities.ConnectionRequest;
import com.vynk.connections.repositories.AcceptedConnectionRepository;
import com.vynk.connections.repositories.ConnectionRequestRepository;
import com.vynk.identity.entities.UserAccount;
import com.vynk.identity.repositories.UserAccountRepository;
import com.vynk.shared.error.ApiException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectionService {
    private final ConnectionRequestRepository requests;
    private final AcceptedConnectionRepository accepted;
    private final UserAccountRepository accounts;

    public ConnectionService(ConnectionRequestRepository requests, AcceptedConnectionRepository accepted,
                             UserAccountRepository accounts) {
        this.requests = requests;
        this.accepted = accepted;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<ConnectionApi.Request> incoming(String actor) {
        return requests.findByRecipientUsernameIgnoreCaseAndStatusOrderByCreatedAtDesc(actor, ConnectionRequestStatus.PENDING)
                .stream().map(r -> new ConnectionApi.Request(r.getId(), r.getSender().getUsername(), r.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectionApi.Request> outgoing(String actor) {
        return requests.findBySenderUsernameIgnoreCaseAndStatusOrderByCreatedAtDesc(actor, ConnectionRequestStatus.PENDING)
                .stream().map(r -> new ConnectionApi.Request(r.getId(), r.getRecipient().getUsername(), r.getCreatedAt())).toList();
    }

    @Transactional
    public void send(String actor, String target) {
        UserAccount from = account(actor);
        UserAccount to = account(target);
        if (from.getId().equals(to.getId())) throw ApiException.badRequest("You cannot connect to yourself.");
        long low = Math.min(from.getId(), to.getId()), high = Math.max(from.getId(), to.getId());
        if (accepted.existsById(new AcceptedConnection.Key(low, high))) throw ApiException.conflict("Already connected.");
        var outgoing = requests.findBySenderUsernameIgnoreCaseAndRecipientUsernameIgnoreCase(actor, target);
        if (outgoing.isPresent()) {
            if (outgoing.get().getStatus() == ConnectionRequestStatus.PENDING)
                throw ApiException.conflict("A pending request already exists.");
            outgoing.get().setStatus(ConnectionRequestStatus.PENDING);
            return;
        }
        var incoming = requests.findBySenderUsernameIgnoreCaseAndRecipientUsernameIgnoreCase(target, actor);
        if (incoming.isPresent() && incoming.get().getStatus() == ConnectionRequestStatus.PENDING)
            throw ApiException.conflict("This person has already requested to connect.");
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
        try {
            accepted.save(new AcceptedConnection(low, high));
            request.setStatus(ConnectionRequestStatus.ACCEPTED);
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

    private ConnectionRequest request(long id) {
        return requests.findById(id).orElseThrow(() -> ApiException.notFound("Connection request not found."));
    }

    private UserAccount account(String username) {
        return accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }
}

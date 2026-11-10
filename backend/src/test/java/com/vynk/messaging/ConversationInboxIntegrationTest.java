package com.vynk.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vynk.auth.controllers.AuthController;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:vynk-inbox;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class ConversationInboxIntegrationTest {
    private static final String PASSWORD = "inbox-test-password";
    private static final AtomicInteger CLIENTS = new AtomicInteger();
    @Autowired private MockMvc mvc;
    @Autowired private EntityManagerFactory entityManagerFactory;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void inboxIsPagedNewestFirstWithUnreadCountsAndHiddenConversations() throws Exception {
        MockHttpSession owner = register("inboxowner", "Owner", "Receiver");
        String[] peers = {"inboxpeera", "inboxpeerb", "inboxpeerc", "inboxpeerd", "inboxpeere"};
        Map<String, MockHttpSession> sessions = new HashMap<>();
        Map<String, Long> conversations = new HashMap<>();
        for (String peer : peers) {
            sessions.put(peer, register(peer, peer, "Peer"));
            connect(owner, peer, sessions.get(peer));
        }
        // Each peer messages the owner once, oldest first: peera ... peere.
        for (String peer : peers) {
            conversations.put(peer, send(sessions.get(peer), "inboxowner", "hi from " + peer));
        }
        mvc.perform(delete("/api/v1/conversations/" + conversations.get("inboxpeerb")).session(owner).with(csrf()))
                .andExpect(status().isNoContent());
        // A new message from peera moves its conversation back to the top, with two unread messages.
        send(sessions.get("inboxpeera"), "inboxowner", "second from peera");

        JsonNode first = inbox(owner, 0, 2);
        assertThat(first.get("totalElements").asLong()).isEqualTo(4);
        assertThat(first.get("totalPages").asInt()).isEqualTo(2);
        assertThat(first.get("hasNext").asBoolean()).isTrue();
        assertThat(usernames(first)).containsExactly("inboxpeera", "inboxpeere");
        assertThat(first.get("items").get(0).get("unreadCount").asLong()).isEqualTo(2);
        assertThat(first.get("items").get(1).get("unreadCount").asLong()).isEqualTo(1);
        assertThat(first.get("items").get(0).get("latestMessage").get("body").asText())
                .isEqualTo("second from peera");

        JsonNode second = inbox(owner, 1, 2);
        assertThat(usernames(second)).containsExactly("inboxpeerd", "inboxpeerc");
        assertThat(second.get("hasNext").asBoolean()).isFalse();
        assertThat(inbox(owner, 2, 2).get("items")).isEmpty();

        // Unread conversations: peera, peerc, peerd, peere (peerb is hidden and excluded).
        unreadSummary(owner, 4);
        mvc.perform(post("/api/v1/conversations/" + conversations.get("inboxpeera") + "/read")
                        .session(owner).with(csrf()))
                .andExpect(status().isNoContent());
        unreadSummary(owner, 3);

        // Sending into a hidden conversation restores it to the inbox.
        send(sessions.get("inboxpeerb"), "inboxowner", "back again");
        JsonNode restored = inbox(owner, 0, 100);
        assertThat(restored.get("totalElements").asLong()).isEqualTo(5);
        assertThat(usernames(restored)).contains("inboxpeerb");

        mvc.perform(get("/api/v1/conversations").session(owner).param("size", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/conversations").session(owner).param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inboxStatementCountDoesNotGrowWithConversations() throws Exception {
        MockHttpSession owner = register("inboxcountowner", "Owner", "Counter");
        for (char letter : new char[] {'a', 'b'}) {
            addConversation(owner, "inboxcountpeer" + letter);
        }
        inbox(owner, 0, 20); // warm up so both measurements start from the same state
        long withTwo = statementsForInbox(owner);

        for (char letter : new char[] {'c', 'd', 'e', 'f', 'g'}) {
            addConversation(owner, "inboxcountpeer" + letter);
        }
        long withSeven = statementsForInbox(owner);

        assertThat(inbox(owner, 0, 20).get("items")).hasSize(7);
        assertThat(withSeven).isEqualTo(withTwo);
    }

    private void addConversation(MockHttpSession owner, String peer) throws Exception {
        MockHttpSession peerSession = register(peer, peer, "Peer");
        connect(owner, peer, peerSession);
        send(peerSession, "inboxcountowner", "hello from " + peer);
    }

    private long statementsForInbox(MockHttpSession owner) throws Exception {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        mvc.perform(get("/api/v1/conversations").session(owner).param("size", "20"))
                .andExpect(status().isOk());
        return stats.getPrepareStatementCount();
    }

    private void unreadSummary(MockHttpSession session, long expected) throws Exception {
        mvc.perform(get("/api/v1/conversations/unread-summary").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadConversations").value(expected));
    }

    private JsonNode inbox(MockHttpSession session, int page, int size) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/conversations").session(session)
                        .param("page", String.valueOf(page)).param("size", String.valueOf(size)))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private static List<String> usernames(JsonNode page) {
        List<String> names = new ArrayList<>();
        page.get("items").forEach(item -> names.add(item.get("participantUsername").asText()));
        return names;
    }

    private long send(MockHttpSession sender, String recipient, String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/conversations/with/" + recipient + "/messages")
                        .session(sender).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", body,
                                "clientRequestId", UUID.randomUUID().toString()))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("conversationId").asLong();
    }

    private void connect(MockHttpSession owner, String peer, MockHttpSession peerSession) throws Exception {
        mvc.perform(post("/api/v1/connections/" + peer + "/requests").session(owner).with(csrf()))
                .andExpect(status().isOk());
        MvcResult incoming = mvc.perform(get("/api/v1/connections/requests/incoming").session(peerSession))
                .andExpect(status().isOk()).andReturn();
        long requestId = mapper.readTree(incoming.getResponse().getContentAsString()).get("items").get(0).get("id").asLong();
        mvc.perform(post("/api/v1/connections/requests/" + requestId + "/accept").session(peerSession).with(csrf()))
                .andExpect(status().isOk());
    }

    private MockHttpSession register(String username, String first, String last) throws Exception {
        // Each simulated user comes from its own address so the auth throttle (per client) is not shared.
        String clientAddress = "198.51.101." + CLIENTS.incrementAndGet();
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(csrf())
                        .with(request -> { request.setRemoteAddr(clientAddress); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AuthController.RegisterRequest(username, PASSWORD, first, last,
                                "Professional", "Inbox integration test"))))
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}

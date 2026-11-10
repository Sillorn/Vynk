package com.vynk.capacity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vynk.auth.controllers.AuthController;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

/**
 * Capacity smoke scenarios for bounded feeds and connection lists on H2.
 *
 * <p>Asserts the bounds that must hold regardless of data volume (page size cap, stable paging,
 * constant statement count per page) and prints a baseline report for the run. Timings are
 * reported, not asserted, so the suite stays stable on shared CI runners.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:vynk-capacity;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class CapacitySmokeTest {
    private static final String PASSWORD = "capacity-test-password";
    private static final AtomicInteger CLIENTS = new AtomicInteger();
    private static final int PUBLIC_POSTS = 120;
    private static final int CONNECTIONS_POSTS = 120;
    private static final int PENDING_REQUESTS = 25;

    @Autowired private MockMvc mvc;
    @Autowired private EntityManagerFactory entityManagerFactory;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> baseline = new ArrayList<>();

    @Test
    void feedsStayBoundedAndStatementCountDoesNotGrowWithData() throws Exception {
        MockHttpSession author = register("capauthor", "Cap", "Author");
        MockHttpSession viewer = register("capviewer", "Cap", "Viewer");

        // Small dataset first, to compare per-page statement counts after growth.
        for (int i = 0; i < 10; i++) {
            createPost(author, "public seed " + i, "PUBLIC");
        }
        // Full pages on both sides: Spring Data skips the count query when a page is short, which
        // would otherwise make the comparison depend on row count rather than on the page size.
        long publicStatementsSmall = statementsFor(get("/api/v1/posts").param("page", "0").param("size", "5"), null);

        for (int i = 10; i < PUBLIC_POSTS; i++) {
            createPost(author, "public seed " + i, "PUBLIC");
        }
        for (int i = 0; i < CONNECTIONS_POSTS; i++) {
            createPost(author, "connections seed " + i, "CONNECTIONS");
        }
        connect(author, "capviewer", viewer);

        // Public feed: page size is capped at 100 and rejected above it, never silently clamped.
        JsonNode full = page(get("/api/v1/posts").param("page", "0").param("size", "100"), null);
        assertThat(full.get("items")).hasSize(100);
        assertThat(full.get("totalElements").asLong()).isEqualTo(PUBLIC_POSTS);
        JsonNode tail = page(get("/api/v1/posts").param("page", "1").param("size", "100"), null);
        assertThat(tail.get("items")).hasSize(PUBLIC_POSTS - 100);
        assertThat(tail.get("hasNext").asBoolean()).isFalse();
        assertThat(page(get("/api/v1/posts").param("page", "9").param("size", "100"), null).get("items")).isEmpty();
        mvc.perform(get("/api/v1/posts").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/posts").param("page", "-1")).andExpect(status().isBadRequest());

        // Connections feed for the connected viewer: includes Connections-only posts, still capped.
        JsonNode viewerFeed = page(get("/api/v1/posts/connections").param("page", "0").param("size", "100"), viewer);
        assertThat(viewerFeed.get("items")).hasSize(100);
        assertThat(viewerFeed.get("totalElements").asLong()).isEqualTo(CONNECTIONS_POSTS);
        assertThat(page(get("/api/v1/posts/connections").param("page", "1").param("size", "100"), viewer)
                .get("items")).hasSize(CONNECTIONS_POSTS - 100);
        mvc.perform(get("/api/v1/posts/connections").session(viewer).param("size", "101"))
                .andExpect(status().isBadRequest());

        // Statements for one page must not grow with the number of rows behind it.
        long publicStatementsLarge = statementsFor(get("/api/v1/posts").param("page", "0").param("size", "5"), null);
        assertThat(publicStatementsLarge).isEqualTo(publicStatementsSmall);

        long connectionsStatements = statementsFor(
                get("/api/v1/posts/connections").param("page", "0").param("size", "5"), viewer);
        baseline.add(String.format("feed statements per 5-item page: public(%d posts)=%d, public(%d posts)=%d, connections(%d posts)=%d",
                10, publicStatementsSmall, PUBLIC_POSTS, publicStatementsLarge, CONNECTIONS_POSTS, connectionsStatements));
        baseline.add(String.format("feed 100-item page time ms (first request, cold): public=%d, connections=%d",
                timeMillis(get("/api/v1/posts").param("page", "0").param("size", "100"), null),
                timeMillis(get("/api/v1/posts/connections").param("page", "0").param("size", "100"), viewer)));
        reportBaseline();
    }

    @Test
    void connectionListsGrowLinearlyAndAreNotYetPaginated() throws Exception {
        // Documents current behaviour until PN-066: /connections, /requests/incoming and
        // /requests/outgoing return unbounded JSON arrays. Asserted so a future change to paging
        // makes this test fail and be updated deliberately.
        MockHttpSession hub = register("caphub", "Cap", "Hub");
        for (int i = 0; i < PENDING_REQUESTS; i++) {
            MockHttpSession sender = register(senderName(i), "Sender", "Number");
            mvc.perform(post("/api/v1/connections/caphub/requests").session(sender).with(csrf()))
                    .andExpect(status().isOk());
        }
        MvcResult incoming = mvc.perform(get("/api/v1/connections/requests/incoming").session(hub))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray()).andReturn();
        int count = mapper.readTree(incoming.getResponse().getContentAsString()).size();
        assertThat(count).isEqualTo(PENDING_REQUESTS);
        baseline.add("incoming requests returned unpaginated: " + count + " items for " + PENDING_REQUESTS + " pending");
        reportBaseline();
    }

    /** Usernames are lowercase letters only (identity invariant), so map the index to letters. */
    private static String senderName(int index) {
        return "capsender" + (char) ('a' + index / 26) + (char) ('a' + index % 26);
    }

    private void reportBaseline() {
        System.out.println("=== PN-054 capacity smoke baseline (H2 in-memory) ===");
        baseline.forEach(line -> System.out.println("  " + line));
    }

    private long createPost(MockHttpSession session, String content, String audience) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/posts").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("content", content, "audience", audience))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void connect(MockHttpSession owner, String peerUsername, MockHttpSession peer) throws Exception {
        mvc.perform(post("/api/v1/connections/" + peerUsername + "/requests").session(owner).with(csrf()))
                .andExpect(status().isOk());
        MvcResult incoming = mvc.perform(get("/api/v1/connections/requests/incoming").session(peer))
                .andExpect(status().isOk()).andReturn();
        long requestId = mapper.readTree(incoming.getResponse().getContentAsString()).get(0).get("id").asLong();
        mvc.perform(post("/api/v1/connections/requests/" + requestId + "/accept").session(peer).with(csrf()))
                .andExpect(status().isOk());
    }

    private JsonNode page(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                          MockHttpSession session) throws Exception {
        if (session != null) request.session(session);
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private long statementsFor(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               MockHttpSession session) throws Exception {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        if (session != null) request.session(session);
        stats.clear();
        mvc.perform(request).andExpect(status().isOk());
        return stats.getPrepareStatementCount();
    }

    private long timeMillis(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                            MockHttpSession session) throws Exception {
        if (session != null) request.session(session);
        long start = System.nanoTime();
        mvc.perform(request).andExpect(status().isOk());
        return (System.nanoTime() - start) / 1_000_000;
    }

    private MockHttpSession register(String username, String first, String last) throws Exception {
        // Each simulated user comes from its own address so the auth throttle (per client) is not shared.
        String clientAddress = "198.51.102." + CLIENTS.incrementAndGet();
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(csrf())
                        .with(request -> { request.setRemoteAddr(clientAddress); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AuthController.RegisterRequest(username, PASSWORD, first, last,
                                "Professional", "Capacity smoke test"))))
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}

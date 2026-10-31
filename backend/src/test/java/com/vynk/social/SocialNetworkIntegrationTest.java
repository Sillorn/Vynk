package com.vynk.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vynk.auth.controllers.AuthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:vynk-social;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class SocialNetworkIntegrationTest {
    private static final String PASSWORD = "social-test-password";
    @Autowired private MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void protectsPostAndCommentOwnershipAndRequiresAcceptedConnectionForConnectionsFeed() throws Exception {
        MockHttpSession alice = register("socialalice", "Alice", "Owner");
        MockHttpSession bob = register("socialbob", "Bob", "Friend");
        MockHttpSession mallory = register("socialmallory", "Mallory", "Visitor");

        long postId = createPost(alice, "public social post");
        mvc.perform(get("/api/v1/posts").param("page", "0").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].content").value("public social post"));
        mvc.perform(get("/api/v1/posts/connections").session(bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());

        mvc.perform(put("/api/v1/posts/" + postId).session(bob).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hijacked\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/posts/" + postId).session(mallory).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/posts/" + postId + "/likes").with(csrf()))
                .andExpect(status().isUnauthorized());

        MvcResult comment = mvc.perform(post("/api/v1/posts/" + postId + "/comments").session(bob).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"nice work\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("socialbob")).andReturn();
        long commentId = mapper.readTree(comment.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/v1/posts/comments/" + commentId).session(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"changed\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/posts/comments/" + commentId).session(bob).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"edited\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("edited"));

        mvc.perform(post("/api/v1/posts/" + postId + "/likes").session(bob).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.liked").value(true)).andExpect(jsonPath("$.likeCount").value(1));
        mvc.perform(post("/api/v1/posts/" + postId + "/likes").session(bob).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.liked").value(false)).andExpect(jsonPath("$.likeCount").value(0));

        mvc.perform(post("/api/v1/connections/socialbob/requests").session(alice).with(csrf()))
                .andExpect(status().isOk());
        MvcResult outgoing = mvc.perform(get("/api/v1/connections/requests/incoming").session(bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value("socialalice")).andReturn();
        long requestId = mapper.readTree(outgoing.getResponse().getContentAsString()).get(0).get("id").asLong();
        mvc.perform(post("/api/v1/connections/requests/" + requestId + "/accept").session(bob).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/posts/connections").session(bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());

        mvc.perform(post("/api/v1/posts/" + postId + "/comments").session(mallory).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"x\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/posts/" + postId + "/comments").param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    private long createPost(MockHttpSession session, String content) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/posts").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(java.util.Map.of("content", content))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private MockHttpSession register(String username, String first, String last) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AuthController.RegisterRequest(username, PASSWORD, first, last,
                                "Professional", "Social integration test"))))
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}

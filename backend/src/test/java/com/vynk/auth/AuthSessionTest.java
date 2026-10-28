package com.vynk.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vynk.auth.controllers.AuthController;
import com.vynk.identity.entities.UserAccount;
import com.vynk.identity.repositories.UserAccountRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:vynk-auth;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class AuthSessionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginSetsSessionAndXsrfCookie() throws Exception {
        UserAccount user = new UserAccount("alice", passwordEncoder.encode("password123"));
        userAccountRepository.save(user);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.LoginRequest("alice", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
    }

    @Test
    void logoutClearsSession() throws Exception {
        UserAccount user = new UserAccount("bob", passwordEncoder.encode("password123"));
        userAccountRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.LoginRequest("bob", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        HttpSession session = loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful());
        assertThat(((org.springframework.mock.web.MockHttpSession) session).isInvalid()).isTrue();
        mockMvc.perform(get("/api/v1/auth/me")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationPasswordChangeAndAccountDeletionLifecycle() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carol","password":"initial-password","firstName":"Carol",
                                 "lastName":"Jones","headline":"Engineer","summary":"Profile summary"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) registration.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/api/v1/auth/password")
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"replacement-password\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/password")
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"initial-password\",\"newPassword\":\"replacement-password\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/logout").session(session).with(csrf()))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"replacement-password\"}"))
                .andExpect(status().isOk());

        MvcResult relogin = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"replacement-password\"}"))
                .andExpect(status().isOk()).andReturn();
        var authenticatedSession = (org.springframework.mock.web.MockHttpSession) relogin.getRequest().getSession(false);
        mockMvc.perform(delete("/api/v1/auth/account").session(authenticatedSession).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(userAccountRepository.existsByUsernameIgnoreCase("carol")).isFalse();
        assertThat(((org.springframework.mock.web.MockHttpSession) authenticatedSession).isInvalid()).isTrue();
        mockMvc.perform(get("/api/v1/auth/me").session(authenticatedSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequestWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());

        UserAccount user = new UserAccount("csrfuser", passwordEncoder.encode("password123"));
        userAccountRepository.save(user);
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"csrfuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .session((org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isUnauthorized());
    }
}

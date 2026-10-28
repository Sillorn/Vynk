package com.vynk.profiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vynk.auth.controllers.AuthController;
import com.vynk.cv.services.CvPdfService;
import com.vynk.identity.entities.Profile;
import com.vynk.identity.entities.UserAccount;
import com.vynk.identity.repositories.ProfileRepository;
import com.vynk.identity.repositories.UserAccountRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:vynk-profile-cv;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ProfileCvIntegrationTest {

    private static final String PASSWORD = "profile-test-password";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountRepository users;
    @Autowired private ProfileRepository profiles;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CvPdfService cvPdfService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void savesComplexProfileValidatesDatesAndKeepsEditsOwnerScoped() throws Exception {
        MockHttpSession alice = register("alice", "Alice", "Owner");
        MockHttpSession bob = register("bobby", "Bob", "Other");

        mockMvc.perform(put("/api/v1/profiles/me/experience").session(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"items":[{"title":"Principal Engineer","company":"Example Corp",
                                "location":"Remote","startDate":"2020-01-01","current":true,
                                "description":"Built a resilient platform"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experience[0].title").value("Principal Engineer"))
                .andExpect(jsonPath("$.experience[0].current").value(true));

        mockMvc.perform(put("/api/v1/profiles/me/education").session(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"items":[{"school":"State University","degree":"MSc",
                                "fieldOfStudy":"Computer Science","startDate":"2016-09-01",
                                "endDate":"2018-06-01"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.education[0].school").value("State University"));

        mockMvc.perform(put("/api/v1/profiles/me/projects").session(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"items":[{"name":"Portfolio App","description":"A Unicode project — café",
                                "url":"https://example.org/project","startDate":"2022-01-01",
                                "endDate":"2023-01-01"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[0].name").value("Portfolio App"));

        mockMvc.perform(put("/api/v1/profiles/me/experience").session(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"items":[{"title":"Invalid Dates","company":"Example",
                                "startDate":"2024-01-01","endDate":"2023-01-01","current":false}]}
                                """))
                .andExpect(status().isBadRequest());

        // Authenticated writes are resolved from the caller's identity, never a supplied username.
        mockMvc.perform(put("/api/v1/profiles/me").session(bob).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"firstName":"Robert","lastName":"Other","headline":"Different",
                                "summary":"Bob's own profile","location":null}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/profiles/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.experience[0].title").value("Principal Engineer"));

    }

    @Test
    void cvIsPublicValidPdfAndContainsProfileDataOnly() throws Exception {
        register("cvuser", "Zoë", "Unicode");
        UserAccount user = users.findByUsernameIgnoreCase("cvuser").orElseThrow();
        Profile profile = profiles.findByUserAccountUsernameIgnoreCase("cvuser").orElseThrow();
        // The exporter reads structured public profile data; use the API to populate these sections.
        MockHttpSession session = login("cvuser");
        mockMvc.perform(put("/api/v1/profiles/me/skills").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"name\":\"Java\"},{\"name\":\"Accessibility\"}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/profiles/me/links").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"label\":\"Portfolio\",\"url\":\"https://example.org\"}]}"))
                .andExpect(status().isOk());

        MvcResult response = mockMvc.perform(get("/api/v1/profiles/cvuser/cv"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.APPLICATION_PDF))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.containsString("cvuser-cv.pdf")))
                .andReturn();
        byte[] pdfBytes = response.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        try (var document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Zoë Unicode", "Java", "Accessibility", "https://example.org");
            assertThat(text).doesNotContain("private-message-content", "social-post-content");
        }
        assertThat(user.getUsername()).isEqualTo("cvuser");
        assertThat(cvPdfService.render("cvuser")).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    void profileWritesRequireAuthenticationAndPublicProfileOmitsEmptySections() throws Exception {
        register("publicuser", "Public", "Person");
        mockMvc.perform(get("/api/v1/profiles/publicuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("publicuser"))
                .andExpect(jsonPath("$.experience").doesNotExist())
                .andExpect(jsonPath("$.skills").doesNotExist());
        mockMvc.perform(put("/api/v1/profiles/me/skills").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession register(String username, String firstName, String lastName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.RegisterRequest(
                                username, PASSWORD, firstName, lastName, "Software Engineer", "Professional summary"))))
                .andExpect(status().isCreated()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.LoginRequest(username, PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}

package com.vynk.shared.openapi;

import com.vynk.shared.api.PageResponse;
import com.vynk.shared.error.FieldViolation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the published OpenAPI document and writes it to {@code target/openapi.json}.
 *
 * <p>The generated artifact is consumed by API clients and uploaded by CI. Assertions here keep
 * the document valid, versioned and populated with the shared contract schemas.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    private static final Path GENERATED_DOCUMENT = Path.of("target", "openapi.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishesVersionedDocumentWithSharedSchemas() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").value(matchesPattern("3\\.[0-9]+\\.[0-9]+")))
                .andExpect(jsonPath("$.info.title").value(OpenApiConfig.TITLE))
                .andExpect(jsonPath("$.info.version").value(OpenApiConfig.API_VERSION))
                .andExpect(jsonPath("$.paths['/api/v1/ping'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ping'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ping'].get.responses['500'].content['application/problem+json']")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.Problem.properties.code.enum").isArray())
                .andExpect(jsonPath("$.components.schemas.Problem.properties.traceId").exists())
                .andExpect(jsonPath("$.components.schemas.Problem.properties.errors.items.$ref")
                        .value("#/components/schemas/FieldViolation"))
                .andExpect(jsonPath("$.components.schemas.FieldViolation.properties.field").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponse.properties.items").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponse.required").isArray())
                .andReturn();

        String document = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(document).startsWith("{");
        writeDocument(document);
        assertThat(GENERATED_DOCUMENT).exists();
    }

    private static void writeDocument(String document) throws IOException {
        Files.createDirectories(GENERATED_DOCUMENT.getParent());
        Files.writeString(GENERATED_DOCUMENT, document, StandardCharsets.UTF_8);
    }
}

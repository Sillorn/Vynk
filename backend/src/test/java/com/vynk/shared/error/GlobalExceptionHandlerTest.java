package com.vynk.shared.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.vynk.shared.tracing.TraceIdFilter;
import com.vynk.shared.tracing.TraceIds;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorController())
                .setControllerAdvice(new GlobalExceptionHandler(new ProblemDetailsFactory()))
                .addFilters(new TraceIdFilter())
                .build();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void rendersApiExceptionAsRfc9457Problem() throws Exception {
        mockMvc.perform(get("/api/v1/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists(TraceIds.HEADER))
                .andExpect(jsonPath("$.type").value("urn:vynk:error:not-found"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("The requested profile does not exist."))
                .andExpect(jsonPath("$.instance").value("/api/v1/test/not-found"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void mapsConflictStatusAndCode() throws Exception {
        mockMvc.perform(get("/api/v1/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void includesNonReservedExceptionExtensions() throws Exception {
        mockMvc.perform(get("/api/v1/test/properties"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.reason").value("ownership"));
    }

    @Test
    void includesFieldViolationsForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Name is required."));
    }

    @Test
    void sanitizesUnexpectedFailures() throws Exception {
        mockMvc.perform(get("/api/v1/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.UNEXPECTED_ERROR_DETAIL))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void returnsTraceIdHeaderAndBodyAndClearsMdc() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/test/not-found")
                        .header(TraceIds.HEADER, "client-trace-123"))
                .andExpect(header().string(TraceIds.HEADER, "client-trace-123"))
                .andExpect(jsonPath("$.traceId").value("client-trace-123"))
                .andReturn();

        assertThat(result.getResponse().getHeader(TraceIds.HEADER)).isEqualTo("client-trace-123");
        assertThat(MDC.get(TraceIds.MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeClientTraceId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/test/not-found")
                        .header(TraceIds.HEADER, "<script>alert(1)</script>"))
                .andExpect(header().string(TraceIds.HEADER, matchesPattern("[0-9a-f]{32}")))
                .andReturn();

        assertThat(result.getResponse().getHeader(TraceIds.HEADER))
                .matches("[0-9a-f]{32}")
                .isNotEqualTo("<script>alert(1)</script>");
    }

    @RestController
    @RequestMapping("/api/v1/test")
    static class ErrorController {

        @GetMapping("/not-found")
        void notFound() {
            throw ApiException.notFound("The requested profile does not exist.");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw ApiException.conflict("That resource already exists.");
        }

        @GetMapping("/properties")
        void properties() {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Not allowed.", Map.of("reason", "ownership"));
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("SQL password=secret");
        }

        record ValidationRequest(@NotBlank(message = "Name is required.") String name) {
        }
    }
}

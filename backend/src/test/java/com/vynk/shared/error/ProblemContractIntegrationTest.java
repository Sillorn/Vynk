package com.vynk.shared.error;

import com.vynk.shared.tracing.TraceIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pingReturnsGeneratedTraceHeader() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIds.HEADER, matchesPattern("[0-9a-f]{32}")));
    }

    @Test
    void preservesWellFormedCallerTraceId() throws Exception {
        mockMvc.perform(get("/api/v1/ping").header(TraceIds.HEADER, "caller-trace-42"))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIds.HEADER, "caller-trace-42"));
    }

    @Test
    void unknownApiRouteReturnsProblemResponse() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists(TraceIds.HEADER))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/api/v1/does-not-exist"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void unsupportedMethodReturnsProblemResponse() throws Exception {
        mockMvc.perform(post("/api/v1/ping"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}

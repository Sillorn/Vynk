package com.vynk.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter();

    private int post(String path, String remoteAddr) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void limitsEachClientAddressIndependently() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.MAX_ATTEMPTS; i++) {
            assertThat(post("/api/v1/auth/login", "203.0.113.10")).isEqualTo(200);
        }
        assertThat(post("/api/v1/auth/login", "203.0.113.10")).isEqualTo(429);
        // A different client is unaffected by the first client's exhausted bucket.
        assertThat(post("/api/v1/auth/login", "203.0.113.11")).isEqualTo(200);
    }

    @Test
    void limitsLoginAndRegisterSeparately() throws Exception {
        for (int i = 0; i <= AuthRateLimitFilter.MAX_ATTEMPTS; i++) {
            post("/api/v1/auth/login", "203.0.113.20");
        }
        assertThat(post("/api/v1/auth/register", "203.0.113.20")).isEqualTo(200);
    }

    @Test
    void ignoresOtherEndpointsAndMethods() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.MAX_ATTEMPTS * 2; i++) {
            assertThat(post("/api/v1/posts", "203.0.113.30")).isEqualTo(200);
        }
    }
}

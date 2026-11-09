package com.vynk.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiCacheControlFilterTest {

    private final ApiCacheControlFilter filter = new ApiCacheControlFilter();

    @Test
    void apiResponsesAreNoStoreEvenWhenCommittedByTheHandler() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/auth/me"), response, (req, res) -> {
            res.getWriter().write("body");
            res.flushBuffer();
        });
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store, max-age=0");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
    }

    @Test
    void nonApiResponsesAreUntouched() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/index.html"), response, (req, res) -> { });
        assertThat(response.getHeader("Cache-Control")).isNull();
    }
}

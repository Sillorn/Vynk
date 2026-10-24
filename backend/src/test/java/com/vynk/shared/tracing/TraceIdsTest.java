package com.vynk.shared.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

class TraceIdsTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void acceptsHeaderSafeClientValue() {
        assertThat(TraceIds.acceptedClientValue("client-trace-123")).contains("client-trace-123");
    }

    @Test
    void rejectsMissingShortWhitespaceOrUnsafeClientValues() {
        assertThat(TraceIds.acceptedClientValue(null)).isEmpty();
        assertThat(TraceIds.acceptedClientValue("short")).isEmpty();
        assertThat(TraceIds.acceptedClientValue("has whitespace")).isEmpty();
        assertThat(TraceIds.acceptedClientValue("<script>alert(1)</script>")).isEmpty();
        assertThat(TraceIds.acceptedClientValue(" bad-leading-space")).contains("bad-leading-space");
        assertThat(TraceIds.acceptedClientValue("plus+sign")).isEmpty();
    }

    @Test
    void generatesLowercaseHexIdentifier() {
        assertThat(TraceIds.generate()).matches("[0-9a-f]{32}");
    }

    @Test
    void ensureUsesActiveMdcTraceAndAttachesItToRequest() {
        MDC.put(TraceIds.MDC_KEY, "mdc-trace-1234");
        MockHttpServletRequest request = new MockHttpServletRequest();

        String traceId = TraceIds.ensure(request);

        assertThat(traceId).isEqualTo("mdc-trace-1234");
        assertThat(request.getAttribute(TraceIds.REQUEST_ATTRIBUTE)).isEqualTo("mdc-trace-1234");
    }

    @Test
    void ensureReusesRequestAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(TraceIds.REQUEST_ATTRIBUTE, "attribute-1234");

        assertThat(TraceIds.ensure(request)).isEqualTo("attribute-1234");
    }
}

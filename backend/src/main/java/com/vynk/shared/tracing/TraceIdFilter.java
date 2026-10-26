package com.vynk.shared.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes a trace ID for every request and echoes it to the caller.
 *
 * <p>A well-formed caller-provided ID is preserved so browser and proxy logs can be correlated.
 * Anything else is replaced with a server-generated ID; arbitrary header content is never
 * reflected or logged.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String traceId = TraceIds.acceptedClientValue(request.getHeader(TraceIds.HEADER))
                .orElseGet(TraceIds::generate);

        request.setAttribute(TraceIds.REQUEST_ATTRIBUTE, traceId);
        response.setHeader(TraceIds.HEADER, traceId);
        MDC.put(TraceIds.MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIds.MDC_KEY);
        }
    }
}

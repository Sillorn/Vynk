package com.vynk.shared.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bounded in-memory throttling for authentication endpoints.
 *
 * <p>The demo runs as a single backend instance, so a process-local counter is sufficient. Limits
 * are intentionally generous for interactive use while still slowing credential probing. Private
 * request content (passwords, usernames) is never logged.</p>
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    static final int MAX_ATTEMPTS = 10;
    static final Duration WINDOW = Duration.ofMinutes(5);

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !("/api/v1/auth/login".equals(path) || "/api/v1/auth/register".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        Attempts entry = attempts.computeIfAbsent(key, ignored -> new Attempts());
        if (entry.isLimited()) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("""
                    {"type":"urn:vynk:error:rate-limited","title":"Too Many Requests","status":429,\
                    "detail":"Too many attempts. Please try again later.","code":"RATE_LIMITED"}""");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static final class Attempts {
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant windowStart = Instant.now();

        boolean isLimited() {
            Instant now = Instant.now();
            if (now.isAfter(windowStart.plus(WINDOW))) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() > MAX_ATTEMPTS;
        }
    }
}

package com.vynk.shared.security;

import com.vynk.shared.error.ApiErrorCode;
import com.vynk.shared.tracing.TraceIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

/**
 * Answers rejected requests (failed CSRF check, denied method security) with 403 and an RFC 9457
 * problem body. A 401 is reserved for missing or expired authentication so clients can tell
 * "sign in again" apart from "this request was refused".
 */
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        ApiErrorCode code = ApiErrorCode.FORBIDDEN;
        String detail = exception instanceof CsrfException
                ? "The request could not be verified. Refresh the page and try again."
                : "You do not have permission to perform this action.";
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"type\":\"" + code.type() + "\",\"title\":\"" + code.title()
                + "\",\"status\":" + code.status().value() + ",\"detail\":\"" + detail
                + "\",\"code\":\"" + code.name() + "\",\"traceId\":\"" + TraceIds.ensure(request) + "\"}");
    }
}

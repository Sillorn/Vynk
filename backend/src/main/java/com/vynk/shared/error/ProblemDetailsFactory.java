package com.vynk.shared.error;

import java.net.URI;
import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Builds RFC 9457 problem documents with the Vynk error contract members.
 */
@Component
public class ProblemDetailsFactory {

    public ProblemDetail create(
            ApiErrorCode code,
            String detail,
            URI instance,
            String traceId,
            List<FieldViolation> violations) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setType(code.type());
        problem.setTitle(code.title());
        problem.setInstance(instance);
        problem.setProperty("code", code.name());
        problem.setProperty("traceId", traceId);
        if (violations != null && !violations.isEmpty()) {
            problem.setProperty("errors", List.copyOf(violations));
        }
        return problem;
    }
}

package com.vynk.shared.error;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vynk.shared.tracing.TraceIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converts expected and unexpected request failures into RFC 9457 problem responses.
 *
 * <p>All responses carry the stable {@code code} and {@code traceId} members. Unexpected failures
 * are logged with their stack trace server-side but never expose exception details to the caller.
 * Handlers in feature packages should throw {@link ApiException} for deliberate failures.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String UNEXPECTED_ERROR_DETAIL =
            "An unexpected error occurred. Provide the trace ID when reporting this problem.";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Set<String> RESERVED_PROPERTIES =
            Set.of("type", "title", "status", "detail", "instance", "code", "traceId", "errors");

    private final ProblemDetailsFactory problems;

    public GlobalExceptionHandler(ProblemDetailsFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        if (exception.code().status().is5xxServerError()) {
            log.error("Request failed with {}", exception.code(), exception);
        } else {
            log.debug("Request rejected with {}: {}", exception.code(), exception.getMessage());
        }
        ProblemDetail problem = respond(exception.code(), detail(exception), request, List.of()).getBody();
        exception.properties().forEach((key, value) -> {
            if (!RESERVED_PROPERTIES.contains(key)) {
                problem.setProperty(key, value);
            }
        });
        return response(exception.code(), problem, TraceIds.ensure(request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleBodyValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        return respond(
                ApiErrorCode.VALIDATION_FAILED,
                "Request validation failed.",
                request,
                violations(exception));
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ProblemDetail> handleBinding(BindException exception, HttpServletRequest request) {
        return respond(
                ApiErrorCode.VALIDATION_FAILED,
                "Request validation failed.",
                request,
                violations(exception));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> handleMethodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        List<FieldViolation> violations = exception.getParameterValidationResults().stream()
                .flatMap(result -> {
                    String name = result.getMethodParameter().getParameterName();
                    String field = name == null || name.isBlank() ? "request" : name;
                    return result.getResolvableErrors().stream()
                            .map(error -> new FieldViolation(field, message(error)));
                })
                .toList();
        return respond(
                ApiErrorCode.VALIDATION_FAILED,
                "Request validation failed.",
                request,
                violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    return new FieldViolation(
                            path.isBlank() ? "_request" : path,
                            message(violation.getMessage()));
                })
                .toList();
        return respond(
                ApiErrorCode.VALIDATION_FAILED,
                "Request validation failed.",
                request,
                violations);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return respond(
                ApiErrorCode.VALIDATION_FAILED,
                "Request validation failed.",
                request,
                List.of(new FieldViolation(
                        exception.getParameterName(), "This request parameter is required.")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return respond(
                ApiErrorCode.VALIDATION_FAILED,
                "Request validation failed.",
                request,
                List.of(new FieldViolation(exception.getName(), "The value has an invalid format.")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return respond(
                ApiErrorCode.BAD_REQUEST,
                "The request body is missing or malformed.",
                request,
                List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return respond(
                ApiErrorCode.METHOD_NOT_ALLOWED,
                "The HTTP method is not supported for this endpoint.",
                request,
                List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return respond(
                ApiErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "The request content type is not supported.",
                request,
                List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ProblemDetail> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception, HttpServletRequest request) {
        return respond(
                ApiErrorCode.NOT_ACCEPTABLE,
                "None of the requested response formats are available.",
                request,
                List.of());
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ProblemDetail> handleNotFound(Exception exception, HttpServletRequest request) {
        return respond(
                ApiErrorCode.NOT_FOUND,
                "The requested resource was not found.",
                request,
                List.of());
    }

    @ExceptionHandler(ErrorResponseException.class)
    ResponseEntity<ProblemDetail> handleErrorResponse(
            ErrorResponseException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status != null && status.is5xxServerError()) {
            log.error("Request failed with status {}", status.value(), exception);
        }
        ApiErrorCode code = codeFor(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status);
        return respond(code, detail(exception), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled request failure", exception);
        return respond(ApiErrorCode.INTERNAL_ERROR, UNEXPECTED_ERROR_DETAIL, request, List.of());
    }

    private ResponseEntity<ProblemDetail> respond(
            ApiErrorCode code,
            String detail,
            HttpServletRequest request,
            List<FieldViolation> violations) {
        String traceId = TraceIds.ensure(request);
        ProblemDetail problem = problems.create(code, detail, instance(request), traceId, violations);
        return response(code, problem, traceId);
    }

    private static ResponseEntity<ProblemDetail> response(
            ApiErrorCode code, ProblemDetail problem, String traceId) {
        return ResponseEntity.status(code.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(TraceIds.HEADER, traceId)
                .body(problem);
    }

    private static List<FieldViolation> violations(BindException exception) {
        List<FieldViolation> violations = new ArrayList<>();
        for (ObjectError error : exception.getBindingResult().getAllErrors()) {
            if (error instanceof FieldError fieldError) {
                violations.add(new FieldViolation(fieldError.getField(), message(fieldError)));
            } else {
                violations.add(new FieldViolation("_request", message(error)));
            }
        }
        return violations;
    }

    private static URI instance(HttpServletRequest request) {
        try {
            return URI.create(request.getRequestURI());
        } catch (IllegalArgumentException exception) {
            return URI.create("/");
        }
    }

    private static String detail(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? UNEXPECTED_ERROR_DETAIL
                : exception.getMessage();
    }

    private static String message(MessageSourceResolvable resolvable) {
        return message(resolvable.getDefaultMessage());
    }

    private static String message(String value) {
        return value == null || value.isBlank() ? "Invalid value." : value;
    }

    private static ApiErrorCode codeFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ApiErrorCode.BAD_REQUEST;
            case UNAUTHORIZED -> ApiErrorCode.UNAUTHENTICATED;
            case FORBIDDEN -> ApiErrorCode.FORBIDDEN;
            case NOT_FOUND -> ApiErrorCode.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> ApiErrorCode.METHOD_NOT_ALLOWED;
            case NOT_ACCEPTABLE -> ApiErrorCode.NOT_ACCEPTABLE;
            case CONFLICT -> ApiErrorCode.CONFLICT;
            case UNSUPPORTED_MEDIA_TYPE -> ApiErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case TOO_MANY_REQUESTS -> ApiErrorCode.RATE_LIMITED;
            default -> status.is5xxServerError() ? ApiErrorCode.INTERNAL_ERROR : ApiErrorCode.BAD_REQUEST;
        };
    }
}

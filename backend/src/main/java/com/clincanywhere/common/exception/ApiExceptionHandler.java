package com.clincanywhere.common.exception;

import com.clincanywhere.common.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

/** Converts application and framework exceptions into consistent RFC 7807 responses. */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> missing(NotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", safeMessage(exception), request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> conflict(ConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflict", safeMessage(exception), request);
    }

    @ExceptionHandler({
            BadRequestException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            BindException.class
    })
    ResponseEntity<ProblemDetail> badRequest(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", validationMessage(exception), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> invalidBody(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + java.util.Objects.toString(error.getDefaultMessage(), "Invalid value"))
                .distinct()
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Request validation failed";
        }
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", message, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> unauthorized(HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid username or password", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> forbidden(HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "Access denied", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integrityConflict(HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflict", "The request conflicts with existing data", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> methodNotAllowed(HttpServletRequest request) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed",
                "This HTTP method is not supported", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> unsupportedMedia(HttpServletRequest request) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type",
                "The request content type is not supported", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ProblemDetail> notAcceptable(HttpServletRequest request) {
        return problem(HttpStatus.NOT_ACCEPTABLE, "Not acceptable",
                "The requested response type is not available", request);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ProblemDetail> routeNotFound(HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Route not found", "No endpoint matches this request", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> responseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        HttpStatusCode status = exception.getStatusCode();
        String title = status instanceof HttpStatus knownStatus
                ? knownStatus.getReasonPhrase()
                : "Request failed";
        String detail = java.util.Objects.toString(
                exception.getReason(), "The request could not be completed");
        return problem(status, title, detail, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception, HttpServletRequest request) {
        String traceId = MDC.get(CorrelationIdFilter.MDC_KEY);
        // Keep exception messages and request data out of logs because requests can contain patient details.
        log.error("Unhandled API error traceId={} exceptionType={}",
                traceId, exception.getClass().getSimpleName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatusCode status, String title, String detail, HttpServletRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(java.util.Objects.requireNonNull(status), detail);
        body.setTitle(title);
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("timestamp", Instant.now().toString());
        body.setProperty("traceId", java.util.Objects.toString(MDC.get(CorrelationIdFilter.MDC_KEY), ""));
        // Keep the legacy field so existing frontend error handling remains compatible.
        body.setProperty("message", detail);
        return ResponseEntity.status(java.util.Objects.requireNonNull(status)).body(body);
    }

    private String validationMessage(Exception exception) {
        if (exception instanceof BadRequestException badRequest) {
            return safeMessage(badRequest);
        }
        if (exception instanceof MissingServletRequestParameterException missing) {
            return "Required parameter is missing: " + missing.getParameterName();
        }
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Invalid value for parameter: " + mismatch.getName();
        }
        if (exception instanceof ConstraintViolationException violations) {
            String message = violations.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ": "
                            + java.util.Objects.toString(violation.getMessage(), "Invalid value"))
                    .distinct()
                    .collect(Collectors.joining("; "));
            return message.isBlank() ? "Request validation failed" : message;
        }
        return "Request body is missing or malformed";
    }

    private String safeMessage(Exception exception) {
        return java.util.Objects.toString(exception.getMessage(), "The request could not be completed");
    }
}

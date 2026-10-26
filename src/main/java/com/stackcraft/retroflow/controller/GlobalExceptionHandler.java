package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotTeamMemberException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Maps exceptions to the standard error body (standards §2.2) and the status
 * policy in decisions.md. Never exposes stack traces or internal class names (§4.2).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INVALID_REQUEST = "Request new: ";

    @ExceptionHandler(InvalidInputException.class)
    ResponseEntity<ApiError> invalidInput(InvalidInputException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NotTeamMemberException.class)
    ResponseEntity<ApiError> notTeamMember(NotTeamMemberException ex) {
        return respond(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(RetroflowException.class)
    ResponseEntity<ApiError> businessRuleViolation(RetroflowException ex) {
        return respond(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST + fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST + "body is missing, malformed, or has a value of the wrong type");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidParameter(MethodArgumentTypeMismatchException ex) {
        return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST + "'%s' is not a valid %s".formatted(ex.getValue(), ex.getName()));
    }

    /** Entity-level bean validation at persist time: the service should have caught this first. */
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraintViolation(ConstraintViolationException ex) {
        String violations = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST + violations);
    }

    /**
     * Spring's own errors (unknown URL, wrong method, ...) keep their status; anything
     * else is a 500 without details. Unexpected exceptions are logged with their stack
     * trace (not swallowed, standards §2.3) because the response deliberately hides it.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            HttpStatus known = HttpStatus.resolve(status.value());
            return respond(status, known != null ? known.getReasonPhrase() : "Request failed");
        }
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private static ResponseEntity<ApiError> respond(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(message, status.value()));
    }

}


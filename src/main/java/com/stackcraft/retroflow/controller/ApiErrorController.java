package com.stackcraft.retroflow.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces Spring Boot's default {@code /error} endpoint (its {@code BasicErrorController} only exists
 * when no other {@link ErrorController} is defined). Errors that never reach a controller, e.g. from a
 * filter or the servlet container, then get the same {@link ApiError} body as everything else.
 * Like the catch-all in {@link GlobalExceptionHandler}, it only returns the reason phrase, never details.
 */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<ApiError> error(HttpServletRequest request) {
        HttpStatus status = statusOf(request);
        return ResponseEntity.status(status).body(ApiError.of(status.getReasonPhrase(), status.value()));
    }

    private static HttpStatus statusOf(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = code instanceof Integer value ? HttpStatus.resolve(value) : null;
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

}


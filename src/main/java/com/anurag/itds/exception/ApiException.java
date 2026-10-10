package com.anurag.itds.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for API errors that should be translated into a clean HTTP response
 * by {@link GlobalExceptionHandler} rather than bubbling up as a stack trace.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

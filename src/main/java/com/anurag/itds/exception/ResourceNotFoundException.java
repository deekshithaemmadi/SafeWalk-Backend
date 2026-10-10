package com.anurag.itds.exception;

import org.springframework.http.HttpStatus;

/**
 * 404 — the requested resource does not exist. Also used when a resource exists
 * but does not belong to the current user, so ownership is never leaked.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}

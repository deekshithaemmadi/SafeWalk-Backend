package com.anurag.itds.exception;

import org.springframework.http.HttpStatus;

/**
 * 403 — the caller is authenticated but not allowed to perform this action
 * (for example, a normal user attempting to moderate a report).
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}

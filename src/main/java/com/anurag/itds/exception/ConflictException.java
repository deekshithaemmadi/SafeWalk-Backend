package com.anurag.itds.exception;

import org.springframework.http.HttpStatus;

/**
 * 409 — a conflict with existing data, e.g. registering with an email or phone
 * that already belongs to another account.
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}

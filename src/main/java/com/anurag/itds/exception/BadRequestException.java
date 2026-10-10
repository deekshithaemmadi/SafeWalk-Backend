package com.anurag.itds.exception;

import org.springframework.http.HttpStatus;

/** 400 — the request was malformed or failed validation/business rules. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

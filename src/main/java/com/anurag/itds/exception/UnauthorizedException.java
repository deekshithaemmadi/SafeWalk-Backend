package com.anurag.itds.exception;

import org.springframework.http.HttpStatus;

/** 401 — authentication failed or credentials are missing/invalid. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}

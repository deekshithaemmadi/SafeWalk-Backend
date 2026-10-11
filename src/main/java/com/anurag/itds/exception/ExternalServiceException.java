package com.anurag.itds.exception;

import org.springframework.http.HttpStatus;

/**
 * A third-party dependency (geocoding / routing) failed, timed out or was
 * unreachable. {@code compatCode} lets legacy clients that inspect an OSRM-style
 * {@code code} field keep working.
 */
public class ExternalServiceException extends ApiException {

    private final String compatCode;

    public ExternalServiceException(HttpStatus status, String message, String compatCode) {
        super(status, message);
        this.compatCode = compatCode;
    }

    public String getCompatCode() {
        return compatCode;
    }
}

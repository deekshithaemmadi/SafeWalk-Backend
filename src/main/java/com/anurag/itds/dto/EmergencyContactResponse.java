package com.anurag.itds.dto;

/** A single emergency contact as returned to the client. */
public record EmergencyContactResponse(
        Long id,
        String name,
        String phone,
        String relation
) {
}

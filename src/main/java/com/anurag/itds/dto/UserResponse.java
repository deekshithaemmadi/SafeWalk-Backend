package com.anurag.itds.dto;

import java.util.List;

/**
 * User profile returned by auth and {@code /api/users/me}. NEVER includes the
 * password (or password hash). Matches the frontend contract
 * {@code { id, name, email, phone, emergencyContacts }}.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        List<EmergencyContactResponse> emergencyContacts
) {
}

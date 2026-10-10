package com.anurag.itds.dto;

/**
 * Authentication response for both {@code /api/auth/login} and
 * {@code /api/auth/register}: a JWT plus the freshly loaded user profile.
 */
public record AuthResponse(
        String token,
        UserResponse user
) {
}

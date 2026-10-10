package com.anurag.itds.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code POST /api/auth/register} request body.
 *
 * <p>Matches the frontend registration payload, which always includes at least one
 * emergency contact (the UI enforces two). Contacts are optional here so the API
 * stays robust, but the service persists whatever valid contacts are supplied.</p>
 *
 * @param name              full name
 * @param email             unique email
 * @param phone             10-digit mobile number
 * @param password          raw password (min 6 chars)
 * @param emergencyContacts contacts to attach at registration time
 */
public record RegisterRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 150, message = "Name must be at most 150 characters.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Please enter a valid email address.")
        @Size(max = 190, message = "Email must be at most 190 characters.")
        String email,

        @NotBlank(message = "Phone number is required.")
        @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits.")
        String phone,

        @NotBlank(message = "Password is required.")
        @Size(min = 6, message = "Password must be at least 6 characters.")
        String password,

        List<EmergencyContactRequest> emergencyContacts
) {
    public RegisterRequest {
        if (emergencyContacts == null) {
            emergencyContacts = new ArrayList<>();
        }
    }
}

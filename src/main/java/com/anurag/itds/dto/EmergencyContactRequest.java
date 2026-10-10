package com.anurag.itds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Emergency contact payload used both when creating/updating a single contact and
 * when attaching contacts at registration time.
 *
 * @param name     contact name
 * @param phone    10-digit mobile number
 * @param relation relationship to the user
 */
public record EmergencyContactRequest(
        @NotBlank(message = "Contact name is required.")
        @Size(max = 150, message = "Contact name must be at most 150 characters.")
        String name,

        @NotBlank(message = "Phone number is required.")
        @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits.")
        String phone,

        @NotBlank(message = "Relationship is required.")
        @Size(max = 80, message = "Relationship must be at most 80 characters.")
        String relation
) {
}

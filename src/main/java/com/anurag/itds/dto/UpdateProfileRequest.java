package com.anurag.itds.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code PUT /api/users/me} request body — lets a user update their own profile.
 * Email/phone remain validated and must stay unique across accounts.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 150, message = "Name must be at most 150 characters.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Please enter a valid email address.")
        @Size(max = 190, message = "Email must be at most 190 characters.")
        String email,

        @NotBlank(message = "Phone number is required.")
        @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits.")
        String phone
) {
}

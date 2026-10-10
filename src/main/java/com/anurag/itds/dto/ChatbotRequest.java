package com.anurag.itds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** {@code POST /api/chatbot/message} request body. */
public record ChatbotRequest(
        @NotBlank(message = "Message is required.")
        @Size(max = 1000, message = "Message must be at most 1000 characters.")
        String message
) {
}

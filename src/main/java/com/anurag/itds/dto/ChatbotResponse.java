package com.anurag.itds.dto;

import java.util.List;

/**
 * {@code POST /api/chatbot/message} response.
 *
 * <p>{@code predefined} is always {@code true}: SafeWalk's assistant is a fixed,
 * rule-based FAQ matcher — not a live AI and not an emergency dispatcher. The UI
 * must label answers accordingly.</p>
 */
public record ChatbotResponse(
        String answer,
        List<String> followUps,
        String matchedId,
        boolean predefined
) {
}

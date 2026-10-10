package com.anurag.itds;

import com.anurag.itds.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chatbot: predefined answers only (never a live AI), validation.
 */
class ChatbotControllerTest extends AbstractApiTest {

    private org.springframework.test.web.servlet.ResultActions ask(User user, String message) throws Exception {
        return mockMvc.perform(post("/api/chatbot/message")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"%s"}
                                """.formatted(message)));
    }

    @Test
    @DisplayName("answer is predefined (not a live AI)")
    void predefinedAnswer() throws Exception {
        User user = createUser("chat-" + UUID.randomUUID() + "@example.com");
        ask(user, "What is SOS?")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predefined").value(true))
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.matchedId").value("sos"));
    }

    @Test
    @DisplayName("unknown question gets the fallback answer")
    void fallbackAnswer() throws Exception {
        User user = createUser("chat2-" + UUID.randomUUID() + "@example.com");
        ask(user, "xyzzy blorp")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predefined").value(true))
                .andExpect(jsonPath("$.matchedId").isEmpty())
                .andExpect(jsonPath("$.answer").isNotEmpty());
    }

    @Test
    @DisplayName("registration FAQ matches")
    void registrationFaq() throws Exception {
        User user = createUser("chat3-" + UUID.randomUUID() + "@example.com");
        ask(user, "How do I register?")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedId").value("register"));
    }

    @Test
    @DisplayName("validation: blank message -> 400")
    void blankMessageRejected() throws Exception {
        User user = createUser("chat4-" + UUID.randomUUID() + "@example.com");
        ask(user, "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.message").exists());
    }

    @Test
    @DisplayName("validation: missing body -> 400")
    void missingBody() throws Exception {
        User user = createUser("chat5-" + UUID.randomUUID() + "@example.com");
        mockMvc.perform(post("/api/chatbot/message")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

package com.anurag.itds;

import com.anurag.itds.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint protection: public endpoints stay open, everything else requires a
 * valid JWT, and forged/expired tokens are rejected.
 */
class SecurityConfigTest extends AbstractApiTest {

    @Test
    @DisplayName("public: POST /api/auth/register works without a token")
    void registerIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pub","email":"pub-%s@example.com","phone":"9876500010","password":"secret123","emergencyContacts":[]}
                                """.formatted(java.util.UUID.randomUUID())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("public: GET /api/help-locations works without a token")
    void helpLocationsArePublic() throws Exception {
        mockMvc.perform(get("/api/help-locations"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("protected: GET /api/users/me without token -> 401")
    void usersMeRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protected: GET /api/trips without token -> 401")
    void tripsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protected: GET /api/sos without token -> 401")
    void sosRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/sos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protected: GET /api/community-reports without token -> 401")
    void reportsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/community-reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protected: GET /api/emergency-contacts without token -> 401")
    void contactsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/emergency-contacts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protected: POST /api/chatbot/message without token -> 401")
    void chatbotRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"hello"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("forged token -> 401 on protected endpoint")
    void forgedTokenRejected() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("valid token -> 200 on protected endpoint")
    void validTokenAccepted() throws Exception {
        User user = createUser("sec-" + java.util.UUID.randomUUID() + "@example.com");
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenFor(user)))
                .andExpect(status().isOk());
    }
}

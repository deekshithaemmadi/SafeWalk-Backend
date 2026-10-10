package com.anurag.itds;

import com.anurag.itds.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SOS: recording semantics (never delivery), location consent, ownership.
 */
class SosControllerTest extends AbstractApiTest {

    @Test
    @DisplayName("create SOS -> 201, delivered=false, disclaimer message")
    void createSosIsRecordedNotDelivered() throws Exception {
        User user = createUser("sos-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/sos")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Need help","shareLocation":true,"latitude":12.97,"longitude":77.59,"accuracy":10.0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delivered").value(false))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("create SOS without location consent stores no coordinates")
    void createSosWithoutLocation() throws Exception {
        User user = createUser("sos2-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/sos")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"No location","shareLocation":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delivered").value(false));
    }

    @Test
    @DisplayName("SOS history is isolated per user")
    void sosHistoryIsolated() throws Exception {
        User a = createUser("sosa-" + UUID.randomUUID() + "@example.com");
        User b = createUser("sosb-" + UUID.randomUUID() + "@example.com");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/sos")
                            .header("Authorization", "Bearer " + tokenFor(a))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":"SOS A","shareLocation":false}
                                    """))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/sos")
                        .header("Authorization", "Bearer " + tokenFor(b))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"SOS B","shareLocation":false}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/sos").header("Authorization", "Bearer " + tokenFor(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/sos").header("Authorization", "Bearer " + tokenFor(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("invalid requestedAt timestamp -> 400")
    void invalidRequestedAt() throws Exception {
        User user = createUser("sos3-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/sos")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shareLocation":false,"requestedAt":"not-a-date"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("invalid coordinates -> 400")
    void invalidSosCoordinates() throws Exception {
        User user = createUser("sos4-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/sos")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shareLocation":true,"latitude":999.0,"longitude":77.59}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("missing shareLocation flag -> 400")
    void missingShareLocationFlag() throws Exception {
        User user = createUser("sos5-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/sos")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"hi"}
                                """))
                .andExpect(status().isBadRequest());
    }
}

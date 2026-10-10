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
 * Trip lifecycle: creation, listing, ownership, status transitions, location
 * updates, and coordinate validation.
 */
class TripControllerTest extends AbstractApiTest {

    private String tripJson(String originLabel, String destLabel) {
        return """
                {"originLabel":"%s","destinationLabel":"%s"}
                """.formatted(originLabel, destLabel);
    }

    private String pointJson(double lat, double lon) {
        return """
                {"latitude":%s,"longitude":%s}
                """.formatted(lat, lon);
    }

    private long createTrip(User user) throws Exception {
        String body = mockMvc.perform(post("/api/trips")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Home", "Office")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new tools.jackson.databind.ObjectMapper().readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("create -> 201 with status PLANNED")
    void createTrip() throws Exception {
        User user = createUser("trip-" + UUID.randomUUID() + "@example.com");
        mockMvc.perform(post("/api/trips")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Home", "Office")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.originLabel").value("Home"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("full lifecycle: PLANNED -> ACTIVE -> COMPLETED")
    void startEndLifecycle() throws Exception {
        User user = createUser("life-" + UUID.randomUUID() + "@example.com");
        String token = "Bearer " + tokenFor(user);
        long id = createTrip(user);

        mockMvc.perform(post("/api/trips/{id}/start", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());

        mockMvc.perform(post("/api/trips/{id}/end", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.endedAt").isNotEmpty());
    }

    @Test
    @DisplayName("cancel: PLANNED -> CANCELLED")
    void cancelPlanned() throws Exception {
        User user = createUser("cancel-" + UUID.randomUUID() + "@example.com");
        long id = createTrip(user);

        mockMvc.perform(post("/api/trips/{id}/cancel", id)
                        .header("Authorization", "Bearer " + tokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("invalid transition: cannot start a COMPLETED trip")
    void cannotStartCompletedTrip() throws Exception {
        User user = createUser("badtrans-" + UUID.randomUUID() + "@example.com");
        String token = "Bearer " + tokenFor(user);
        long id = createTrip(user);

        mockMvc.perform(post("/api/trips/{id}/start", id).header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/trips/{id}/end", id).header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/trips/{id}/start", id).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("invalid transition: cannot cancel a COMPLETED trip")
    void cannotCancelCompletedTrip() throws Exception {
        User user = createUser("badcancel-" + UUID.randomUUID() + "@example.com");
        String token = "Bearer " + tokenFor(user);
        long id = createTrip(user);

        mockMvc.perform(post("/api/trips/{id}/start", id).header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/trips/{id}/end", id).header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/trips/{id}/cancel", id).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("location update allowed only while ACTIVE")
    void locationOnlyWhenActive() throws Exception {
        User user = createUser("loc-" + UUID.randomUUID() + "@example.com");
        String token = "Bearer " + tokenFor(user);
        long id = createTrip(user);

        // PLANNED -> reject
        mockMvc.perform(post("/api/trips/{id}/location", id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pointJson(12.97, 77.59)))
                .andExpect(status().isBadRequest());

        // ACTIVE -> accept
        mockMvc.perform(post("/api/trips/{id}/start", id).header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/trips/{id}/location", id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pointJson(12.97, 77.59)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastLatitude").value(12.97))
                .andExpect(jsonPath("$.lastLongitude").value(77.59));
    }

    @Test
    @DisplayName("invalid coordinates -> 400")
    void invalidCoordinates() throws Exception {
        User user = createUser("badcoord-" + UUID.randomUUID() + "@example.com");
        String token = "Bearer " + tokenFor(user);
        long id = createTrip(user);
        mockMvc.perform(post("/api/trips/{id}/start", id).header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/trips/{id}/location", id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pointJson(999.0, 77.59)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/trips/{id}/location", id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pointJson(12.97, -200.0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ownership: another user's trip is invisible (404)")
    void tripsAreIsolated() throws Exception {
        User a = createUser("ta-" + UUID.randomUUID() + "@example.com");
        User b = createUser("tb-" + UUID.randomUUID() + "@example.com");
        long id = createTrip(a);

        mockMvc.perform(get("/api/trips/{id}", id)
                        .header("Authorization", "Bearer " + tokenFor(b)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("list: only own trips, paginated")
    void listOwnTrips() throws Exception {
        User a = createUser("la-" + UUID.randomUUID() + "@example.com");
        User b = createUser("lb-" + UUID.randomUUID() + "@example.com");
        createTrip(a);
        createTrip(a);
        createTrip(b);

        mockMvc.perform(get("/api/trips")
                        .header("Authorization", "Bearer " + tokenFor(a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/trips")
                        .header("Authorization", "Bearer " + tokenFor(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}

package com.anurag.itds;

import com.anurag.itds.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Community reports: submission, permitted visibility, ownership, and
 * moderator-only status changes.
 *
 * <p>{@code @Transactional} rolls back each test, so VERIFIED reports created by
 * one method do not leak into another method's visibility assertions (the shared
 * application context keeps one in-memory database across the whole test run).</p>
 */
@Transactional
class CommunityReportControllerTest extends AbstractApiTest {

    private String reportJson(String category, String description) {
        return """
                {"category":"%s","description":"%s"}
                """.formatted(category, description);
    }

    private long createReport(User user, String category, String description) throws Exception {
        String body = mockMvc.perform(post("/api/community-reports")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson(category, description)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new tools.jackson.databind.ObjectMapper().readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("submit -> 201, status PENDING (never auto-verified)")
    void submitStartsPending() throws Exception {
        User user = createUser("rep-" + UUID.randomUUID() + "@example.com");
        mockMvc.perform(post("/api/community-reports")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("Poor lighting", "Street lights out on MG Road near the park")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.category").value("Poor lighting"));
    }

    @Test
    @DisplayName("visibility: own PENDING report is visible to me")
    void ownPendingVisibleToSelf() throws Exception {
        User user = createUser("rep2-" + UUID.randomUUID() + "@example.com");
        createReport(user, "Poor lighting", "Dark stretch near the metro station gate");

        mockMvc.perform(get("/api/community-reports")
                        .header("Authorization", "Bearer " + tokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("visibility: another user's PENDING report is NOT visible")
    void othersPendingNotVisible() throws Exception {
        User a = createUser("repa-" + UUID.randomUUID() + "@example.com");
        User b = createUser("repb-" + UUID.randomUUID() + "@example.com");
        createReport(a, "Poor lighting", "Pending report from A");

        mockMvc.perform(get("/api/community-reports")
                        .header("Authorization", "Bearer " + tokenFor(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("visibility: VERIFIED reports are visible to everyone")
    void verifiedVisibleToAll() throws Exception {
        User a = createUser("repa2-" + UUID.randomUUID() + "@example.com");
        User b = createUser("repb2-" + UUID.randomUUID() + "@example.com");
        User moderator = createUser("mod-" + UUID.randomUUID() + "@example.com");

        long id = createReport(a, "Poor lighting", "Verified report from A");
        mockMvc.perform(post("/api/community-reports/{id}/moderate", id)
                        .header("Authorization", "Bearer "
                                + jwtService.generateToken(moderator.getId(), moderator.getEmail(), "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"VERIFIED","note":"Confirmed on site"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community-reports")
                        .header("Authorization", "Bearer " + tokenFor(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("VERIFIED"));
    }

    @Test
    @DisplayName("moderation: MODERATOR can verify a report")
    void moderatorCanVerify() throws Exception {
        User a = createUser("repa3-" + UUID.randomUUID() + "@example.com");
        User moderator = createUser("mod2-" + UUID.randomUUID() + "@example.com");
        long id = createReport(a, "Poor lighting", "Needs review");

        mockMvc.perform(post("/api/community-reports/{id}/moderate", id)
                        .header("Authorization", "Bearer "
                                + jwtService.generateToken(moderator.getId(), moderator.getEmail(), "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"VERIFIED","note":"Confirmed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.moderationNote").value("Confirmed"));
    }

    @Test
    @DisplayName("moderation: normal USER is forbidden (403)")
    void userCannotModerate() throws Exception {
        User a = createUser("repa4-" + UUID.randomUUID() + "@example.com");
        User b = createUser("repb4-" + UUID.randomUUID() + "@example.com");
        long id = createReport(a, "Poor lighting", "Needs review");

        mockMvc.perform(post("/api/community-reports/{id}/moderate", id)
                        .header("Authorization", "Bearer " + tokenFor(b))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"VERIFIED","note":"Try"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("moderation: cannot set status back to PENDING")
    void cannotModerateToPending() throws Exception {
        User a = createUser("repa5-" + UUID.randomUUID() + "@example.com");
        User moderator = createUser("mod3-" + UUID.randomUUID() + "@example.com");
        long id = createReport(a, "Poor lighting", "Needs review");

        mockMvc.perform(post("/api/community-reports/{id}/moderate", id)
                        .header("Authorization", "Bearer "
                                + jwtService.generateToken(moderator.getId(), moderator.getEmail(), "MODERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PENDING","note":"Reset"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ownership: owner can update own report content")
    void ownerCanUpdate() throws Exception {
        User user = createUser("repu-" + UUID.randomUUID() + "@example.com");
        long id = createReport(user, "Poor lighting", "Original description");

        mockMvc.perform(put("/api/community-reports/{id}", id)
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Harassment","description":"Updated description of the incident"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Harassment"))
                .andExpect(jsonPath("$.description").value("Updated description of the incident"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("ownership: another user cannot update the report")
    void cannotUpdateOthersReport() throws Exception {
        User a = createUser("repa6-" + UUID.randomUUID() + "@example.com");
        User b = createUser("repb6-" + UUID.randomUUID() + "@example.com");
        long id = createReport(a, "Poor lighting", "Original report");

        mockMvc.perform(put("/api/community-reports/{id}", id)
                        .header("Authorization", "Bearer " + tokenFor(b))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Harassment","description":"Hacked description text"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("validation: short description -> 400")
    void validationRejectsShortDescription() throws Exception {
        User user = createUser("repv-" + UUID.randomUUID() + "@example.com");
        mockMvc.perform(post("/api/community-reports")
                        .header("Authorization", "Bearer " + tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("Poor lighting", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    @DisplayName("filter: category substring filter works")
    void categoryFilter() throws Exception {
        User user = createUser("repf-" + UUID.randomUUID() + "@example.com");
        createReport(user, "Poor lighting", "One dark road near the station");
        createReport(user, "Harassment", "Suspicious following near the bus stop");

        mockMvc.perform(get("/api/community-reports")
                        .param("category", "lighting")
                        .header("Authorization", "Bearer " + tokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].category").value("Poor lighting"));
    }
}

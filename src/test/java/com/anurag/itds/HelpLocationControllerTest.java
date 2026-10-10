package com.anurag.itds;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Help locations: public read-only access, filter validation, pagination.
 * (No data is seeded, so lists are empty — the endpoint must never fabricate
 * locations.)
 */
class HelpLocationControllerTest extends AbstractApiTest {

    @Test
    @DisplayName("public: accessible without a token")
    void publicAccess() throws Exception {
        mockMvc.perform(get("/api/help-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("no fabricated data: empty page when nothing imported")
    void emptyWhenNoData() throws Exception {
        mockMvc.perform(get("/api/help-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("invalid type filter -> 400")
    void invalidTypeRejected() throws Exception {
        mockMvc.perform(get("/api/help-locations").param("type", "MALL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("valid type filter accepted")
    void validTypeAccepted() throws Exception {
        mockMvc.perform(get("/api/help-locations").param("type", "HOSPITAL"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("pagination params accepted")
    void paginationParams() throws Exception {
        mockMvc.perform(get("/api/help-locations").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    @DisplayName("search query param accepted")
    void searchQuery() throws Exception {
        mockMvc.perform(get("/api/help-locations").param("q", "district"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("district filter accepted")
    void districtFilter() throws Exception {
        mockMvc.perform(get("/api/help-locations").param("district", "Central"))
                .andExpect(status().isOk());
    }
}

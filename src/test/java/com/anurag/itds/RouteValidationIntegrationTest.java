package com.anurag.itds;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uses the real MapsService. Test config points the upstream URLs at an unreachable
 * local port, so validation must reject bad input BEFORE any network call, and valid
 * input must surface a clean 503 instead of a fake empty result.
 */
class RouteValidationIntegrationTest extends AbstractApiTest {

    @Test
    void outOfRangeLatitudeIs400() throws Exception {
        mockMvc.perform(get("/api/route").param("startLat", "95").param("startLon", "78.48")
                        .param("endLat", "17.44").param("endLon", "78.34"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'startLat' must be a latitude between -90 and 90."));
    }

    @Test
    void identicalStartAndEndIs400() throws Exception {
        mockMvc.perform(get("/api/route").param("startLat", "17.4").param("startLon", "78.4")
                        .param("endLat", "17.4").param("endLon", "78.4"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankGeocodeQueryIs400() throws Exception {
        mockMvc.perform(get("/api/geocode").param("q", "  "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unreachableRoutingServiceIs503() throws Exception {
        mockMvc.perform(get("/api/route").param("startLat", "17.38").param("startLon", "78.48")
                        .param("endLat", "17.44").param("endLon", "78.34"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("BackendRoutingError"))
                .andExpect(jsonPath("$.routes").isArray());
    }
}

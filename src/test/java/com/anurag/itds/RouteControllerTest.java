package com.anurag.itds;

import com.anurag.itds.exception.ExternalServiceException;
import com.anurag.itds.service.MapsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API-level behaviour of /api/geocode and /api/route (upstream mocked). */
class RouteControllerTest extends AbstractApiTest {

    @MockitoBean
    private MapsService mapsService;

    @Test
    void geocodeIsPublicAndPassesThroughJson() throws Exception {
        when(mapsService.geocode("Charminar")).thenReturn("[{\"display_name\":\"Charminar\"}]");
        mockMvc.perform(get("/api/geocode").param("q", "Charminar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].display_name").value("Charminar"));
    }

    @Test
    void geocodeMissingQueryIs400() throws Exception {
        mockMvc.perform(get("/api/geocode"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'q'."))
                .andExpect(jsonPath("$.path").value("/api/geocode"));
    }

    @Test
    void routeIsPublicAndPassesThroughJson() throws Exception {
        when(mapsService.route(17.38, 78.48, 17.44, 78.34)).thenReturn("{\"code\":\"Ok\",\"routes\":[{}]}");
        mockMvc.perform(get("/api/route")
                        .param("startLat", "17.38").param("startLon", "78.48")
                        .param("endLat", "17.44").param("endLon", "78.34"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("Ok"));
    }

    @Test
    void routeMissingParameterIs400() throws Exception {
        mockMvc.perform(get("/api/route").param("startLat", "17.38").param("startLon", "78.48").param("endLat", "17.44"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'endLon'."));
    }

    @Test
    void routeNonNumericParameterIs400() throws Exception {
        mockMvc.perform(get("/api/route").param("startLat", "abc").param("startLon", "78.48")
                        .param("endLat", "17.44").param("endLon", "78.34"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'startLat'."));
    }

    @Test
    void routeUpstreamTimeoutIs504WithLegacyFields() throws Exception {
        when(mapsService.route(17.38, 78.48, 17.44, 78.34)).thenThrow(new ExternalServiceException(
                HttpStatus.GATEWAY_TIMEOUT, "Routing service timed out. Please try again.", MapsService.ROUTING_ERROR_CODE));
        mockMvc.perform(get("/api/route")
                        .param("startLat", "17.38").param("startLon", "78.48")
                        .param("endLat", "17.44").param("endLon", "78.34"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504))
                .andExpect(jsonPath("$.message").value("Routing service timed out. Please try again."))
                .andExpect(jsonPath("$.code").value("BackendRoutingError"))
                .andExpect(jsonPath("$.routes").isEmpty());
    }

    @Test
    void geocodeUpstreamUnavailableIs503() throws Exception {
        when(mapsService.geocode(anyString())).thenThrow(new ExternalServiceException(
                HttpStatus.SERVICE_UNAVAILABLE, "Geocoding service is currently unavailable. Please try again later.", null));
        mockMvc.perform(get("/api/geocode").param("q", "Ameerpet"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Geocoding service is currently unavailable. Please try again later."))
                .andExpect(jsonPath("$.code").doesNotExist());
    }
}

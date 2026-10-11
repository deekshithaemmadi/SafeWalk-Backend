package com.anurag.itds;

import com.anurag.itds.entity.HelpLocation;
import com.anurag.itds.entity.HelpLocationType;
import com.anurag.itds.repository.HelpLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Help-location endpoints against a small set of clearly fictional test records
 * (rolled back after each test, so other tests still see an empty directory).
 */
@Transactional
class HelpLocationDataTest extends AbstractApiTest {

    @Autowired
    private HelpLocationRepository repository;

    private HelpLocation hospital;

    @BeforeEach
    void seed() {
        hospital = save(HelpLocationType.HOSPITAL, "Test Hospital Alpha", "Hyderabad", "Ameerpet", 17.4375, 78.4483, "test-dataset-a");
        save(HelpLocationType.POLICE, "Test Police Station Beta", "Hyderabad", "Ameerpet", 17.4400, 78.4500, "test-dataset-b");
        save(HelpLocationType.POLICE, "Test Police Station Gamma", "Warangal", "Hanamkonda", 18.0000, 79.5800, "test-dataset-b");
        save(HelpLocationType.WOMEN_HELP, "Test Sakhi Centre Delta", "Hyderabad", "Gachibowli", null, null, "test-dataset-c");
    }

    private HelpLocation save(HelpLocationType type, String name, String district, String locality,
                              Double lat, Double lon, String source) {
        HelpLocation h = new HelpLocation(type, name, "test|" + type + "|" + name);
        h.setDistrict(district);
        h.setLocality(locality);
        h.setLatitude(lat);
        h.setLongitude(lon);
        h.setSource(source);
        h.setSourceYear("2024");
        return repository.save(h);
    }

    @Test
    void districtFilterCombinesWithTypeAndText() throws Exception {
        mockMvc.perform(get("/api/help-locations").param("type", "POLICE").param("district", "hyderabad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Test Police Station Beta"));

        mockMvc.perform(get("/api/help-locations").param("district", "Hyderabad").param("q", "sakhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].hasCoordinates").value(false));
    }

    @Test
    void getByIdAndNotFound() throws Exception {
        mockMvc.perform(get("/api/help-locations/{id}", hospital.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Hospital Alpha"))
                .andExpect(jsonPath("$.source").value("test-dataset-a"))
                .andExpect(jsonPath("$.sourceYear").value("2024"));
        mockMvc.perform(get("/api/help-locations/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Help location not found."));
    }

    @Test
    void nearbyReturnsOnlyRecordsWithCoordinatesSortedByDistance() throws Exception {
        mockMvc.perform(get("/api/help-locations/nearby").param("lat", "17.4375").param("lon", "78.4483").param("radiusKm", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].location.name").value("Test Hospital Alpha"))
                .andExpect(jsonPath("$[0].distanceKm").value(0.0))
                .andExpect(jsonPath("$[1].location.name").value("Test Police Station Beta"));

        mockMvc.perform(get("/api/help-locations/nearby").param("lat", "17.4375").param("lon", "78.4483")
                        .param("radiusKm", "10").param("type", "POLICE").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].location.type").value("POLICE"));
    }

    @Test
    void nearbyValidatesInput() throws Exception {
        mockMvc.perform(get("/api/help-locations/nearby").param("lat", "120").param("lon", "78.4"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/help-locations/nearby").param("lat", "17.4").param("lon", "78.4").param("radiusKm", "500"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/help-locations/nearby").param("lat", "17.4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'lon'."));
    }

    @Test
    void metaDescribesDatasetHonestly() throws Exception {
        mockMvc.perform(get("/api/help-locations/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(4))
                .andExpect(jsonPath("$.countsByType.HOSPITAL").value(1))
                .andExpect(jsonPath("$.countsByType.POLICE").value(2))
                .andExpect(jsonPath("$.countsByType.WOMEN_HELP").value(1))
                .andExpect(jsonPath("$.recordsWithCoordinates").value(3))
                .andExpect(jsonPath("$.sources", hasItem("test-dataset-b")))
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.dataNature").value("STATIC_DATASET"))
                .andExpect(jsonPath("$.limitations").isNotEmpty());
    }
}

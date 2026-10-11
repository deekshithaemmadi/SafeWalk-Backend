package com.anurag.itds;

import com.anurag.itds.dataset.DatasetImporter;
import com.anurag.itds.entity.HelpLocationType;
import com.anurag.itds.repository.HelpLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class SafetyDataControllerTest extends AbstractApiTest {

    @Autowired
    DatasetImporter importer;

    @Autowired
    HelpLocationRepository helpLocationRepository;

    @BeforeEach
    void importDatasets() {
        importer.importAll();
    }

    @Test
    void womenCrimeStatsArePublicAndClearlyLabelledAsStatic() throws Exception {
        mockMvc.perform(get("/api/safety-data/women-crime-stats").param("state", "telangana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(11)))
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.dataNature").value("STATIC_DATASET"))
                .andExpect(jsonPath("$.granularity").value("STATE_ANNUAL"))
                .andExpect(jsonPath("$.limitations", hasItem(org.hamcrest.Matchers.containsString("safety score"))))
                .andExpect(jsonPath("$.records[0].year").value(2011))
                .andExpect(jsonPath("$.records[0].reportedSeparately").value(false))
                .andExpect(jsonPath("$.records[0].total").value(nullValue()))
                .andExpect(jsonPath("$.records[0].counts.RAPE").value(nullValue()))
                .andExpect(jsonPath("$.records[3].year").value(2014))
                .andExpect(jsonPath("$.records[3].total").value(14136))
                .andExpect(jsonPath("$.records[3].counts.RAPE").value(979))
                .andExpect(jsonPath("$.records[3].possiblyIncomplete").value(false))
                .andExpect(jsonPath("$.records[9].year").value(2020))
                .andExpect(jsonPath("$.records[9].possiblyIncomplete").value(true));
    }

    @Test
    void yearRangeFilterAndValidation() throws Exception {
        mockMvc.perform(get("/api/safety-data/women-crime-stats").param("fromYear", "2012").param("toYear", "2013"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(4)))
                .andExpect(jsonPath("$.records[*].year", everyItem(org.hamcrest.Matchers.isOneOf(2012, 2013))));
        mockMvc.perform(get("/api/safety-data/women-crime-stats").param("fromYear", "2016").param("toYear", "2015"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'fromYear' must not be after 'toYear'."));
        mockMvc.perform(get("/api/safety-data/women-crime-stats").param("fromYear", "1800"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/safety-data/women-crime-stats").param("fromYear", "abc"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/safety-data/women-crime-stats").param("state", "Kerala"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(0)));
    }

    @Test
    void sourcesListImportedAndNotImportedDatasets() throws Exception {
        mockMvc.perform(get("/api/safety-data/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].imported").value(true))
                .andExpect(jsonPath("$[0].records", greaterThan(1200)))
                .andExpect(jsonPath("$[1].records").value((int) helpLocationRepository.countByType(HelpLocationType.POLICE)))
                .andExpect(jsonPath("$[3].records").value(126))
                .andExpect(jsonPath("$[4].imported").value(false))
                .andExpect(jsonPath("$[4].notes", is(org.hamcrest.Matchers.startsWith("Not imported"))));
    }

    @Test
    void importedDirectoryIsServedByHelpLocationEndpoints() throws Exception {
        mockMvc.perform(get("/api/help-locations").param("type", "WOMEN_HELP").param("q", "Safa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem("Safa")));
        mockMvc.perform(get("/api/help-locations/nearby").param("lat", "17.4242724").param("lon", "78.5567621")
                        .param("radiusKm", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].location.name").value("SHIVA NARSING HOME"))
                .andExpect(jsonPath("$[0].distanceKm").value(0.0))
                .andExpect(jsonPath("$[*].location.type", everyItem(is("HOSPITAL"))));
        mockMvc.perform(get("/api/help-locations/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countsByType.WOMEN_HELP").value(169))
                .andExpect(jsonPath("$.live").value(false));
    }
}

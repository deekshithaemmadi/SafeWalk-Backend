package com.anurag.itds.controller;

import com.anurag.itds.dto.DatasetSourceResponse;
import com.anurag.itds.dto.WomenCrimeStatsResponse;
import com.anurag.itds.service.SafetyDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Read-only access to static, dataset-derived safety information (never live data). */
@RestController
@RequestMapping("/api/safety-data")
public class SafetyDataController {

    private final SafetyDataService safetyDataService;

    public SafetyDataController(SafetyDataService safetyDataService) {
        this.safetyDataService = safetyDataService;
    }

    @GetMapping("/women-crime-stats")
    public WomenCrimeStatsResponse womenCrimeStats(
            @RequestParam(required = false) @Size(max = 80) String state,
            @RequestParam(required = false) @Min(1900) @Max(2100) Integer fromYear,
            @RequestParam(required = false) @Min(1900) @Max(2100) Integer toYear) {
        return safetyDataService.womenCrimeStats(state, fromYear, toYear);
    }

    @GetMapping("/sources")
    public List<DatasetSourceResponse> sources() {
        return safetyDataService.sources();
    }
}

package com.anurag.itds.dto;

import java.util.List;
import java.util.Map;

public record WomenCrimeStatsResponse(
        List<YearRecord> records,
        Map<String, String> categoryLabels,
        String granularity,
        String dataNature,
        boolean live,
        String source,
        List<String> limitations) {

    public record YearRecord(
            String state,
            String regionContext,
            int year,
            boolean reportedSeparately,
            boolean possiblyIncomplete,
            Map<String, Integer> counts,
            Integer total) {
    }
}

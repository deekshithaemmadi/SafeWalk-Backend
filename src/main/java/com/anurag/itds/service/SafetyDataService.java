package com.anurag.itds.service;

import com.anurag.itds.dataset.DatasetImporter;
import com.anurag.itds.dto.DatasetSourceResponse;
import com.anurag.itds.dto.WomenCrimeStatsResponse;
import com.anurag.itds.dto.WomenCrimeStatsResponse.YearRecord;
import com.anurag.itds.entity.HelpLocationType;
import com.anurag.itds.entity.WomenCrimeStatistic;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.repository.HelpLocationRepository;
import com.anurag.itds.repository.WomenCrimeStatisticRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SafetyDataService {

    /** A year whose total is below this fraction of the state's median total is flagged as possibly incomplete. */
    static final double INCOMPLETE_FRACTION = 0.25;

    private static final Map<String, String> CATEGORY_LABELS = new LinkedHashMap<>();

    static {
        CATEGORY_LABELS.put("RAPE", "Rape");
        CATEGORY_LABELS.put("KIDNAPPING_AND_ABDUCTION", "Kidnapping & abduction");
        CATEGORY_LABELS.put("DOWRY_DEATHS", "Dowry deaths");
        CATEGORY_LABELS.put("CRUELTY_BY_HUSBAND_OR_RELATIVES", "Cruelty by husband or his relatives");
        CATEGORY_LABELS.put("HUMAN_TRAFFICKING", "Human trafficking");
        CATEGORY_LABELS.put("ASSAULT_ON_MODESTY", "Assault on modesty");
        CATEGORY_LABELS.put("SEXUAL_HARASSMENT", "Sexual harassment");
        CATEGORY_LABELS.put("IMMORAL_TRAFFIC", "Immoral traffic");
    }

    private static final List<String> CRIME_LIMITATIONS = List.of(
            "Historical annual state-level counts from a static dataset; not live, not real-time and not location-specific.",
            "These figures must not be used as a safety score for any route, area or place.",
            "Before 2014 Telangana was part of undivided Andhra Pradesh; those Telangana rows are not reported separately (counts null).",
            "Category columns are those present in the source and do not necessarily add up to the reported total.",
            "Years flagged possiblyIncomplete have totals far below the state's other years in the source file (e.g. 2020-2021) and should be verified before use.",
            "Counts reflect registered cases, which depend on reporting rates.");

    private final WomenCrimeStatisticRepository crimeRepository;
    private final HelpLocationRepository helpLocationRepository;

    public SafetyDataService(WomenCrimeStatisticRepository crimeRepository,
                             HelpLocationRepository helpLocationRepository) {
        this.crimeRepository = crimeRepository;
        this.helpLocationRepository = helpLocationRepository;
    }

    public WomenCrimeStatsResponse womenCrimeStats(String state, Integer fromYear, Integer toYear) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            throw new BadRequestException("'fromYear' must not be after 'toYear'.");
        }
        String stateFilter = state == null || state.isBlank() ? null : state.trim().toLowerCase(Locale.ROOT);

        Map<String, List<WomenCrimeStatistic>> byStateYear = new LinkedHashMap<>();
        for (WomenCrimeStatistic s : crimeRepository.findAllByOrderByStateAscYearAsc()) {
            byStateYear.computeIfAbsent(s.getState() + "\u0001" + s.getYear(), k -> new ArrayList<>()).add(s);
        }

        Map<String, List<Integer>> totalsByState = new LinkedHashMap<>();
        List<YearRecord> all = new ArrayList<>();
        for (List<WomenCrimeStatistic> group : byStateYear.values()) {
            WomenCrimeStatistic first = group.get(0);
            Map<String, Integer> counts = new LinkedHashMap<>();
            CATEGORY_LABELS.keySet().forEach(c -> counts.put(c, null));
            Integer total = null;
            for (WomenCrimeStatistic s : group) {
                if ("TOTAL".equals(s.getCategory())) {
                    total = s.getCaseCount();
                } else {
                    counts.put(s.getCategory(), s.getCaseCount());
                }
            }
            boolean reported = total != null || counts.values().stream().anyMatch(v -> v != null);
            if (total != null) {
                totalsByState.computeIfAbsent(first.getState(), k -> new ArrayList<>()).add(total);
            }
            all.add(new YearRecord(first.getState(), first.getRegionContext(), first.getYear(), reported, false,
                    counts, total));
        }

        List<YearRecord> records = new ArrayList<>();
        for (YearRecord r : all) {
            if (stateFilter != null && !r.state().toLowerCase(Locale.ROOT).equals(stateFilter)) {
                continue;
            }
            if ((fromYear != null && r.year() < fromYear) || (toYear != null && r.year() > toYear)) {
                continue;
            }
            boolean incomplete = r.total() != null
                    && r.total() < INCOMPLETE_FRACTION * median(totalsByState.get(r.state()));
            records.add(new YearRecord(r.state(), r.regionContext(), r.year(), r.reportedSeparately(), incomplete,
                    r.counts(), r.total()));
        }
        return new WomenCrimeStatsResponse(records, CATEGORY_LABELS, "STATE_ANNUAL", "STATIC_DATASET", false,
                DatasetImporter.CRIME_SOURCE, CRIME_LIMITATIONS);
    }

    public List<DatasetSourceResponse> sources() {
        return List.of(
                new DatasetSourceResponse("Hyderabad hospitals & health facilities", "hyderabad_hospitals.csv", true,
                        helpLocationRepository.countByType(HelpLocationType.HOSPITAL), "/api/help-locations?type=HOSPITAL",
                        "Hyderabad Hospitals 2018 and Health Facilities 2025 lists with coordinates. Same-name entries "
                                + "within 50 m in one source are de-duplicated; the two source lists are not merged."),
                new DatasetSourceResponse("Hyderabad police stations", "hyderabad_police_stations.csv", true,
                        helpLocationRepository.countByType(HelpLocationType.POLICE), "/api/help-locations?type=POLICE",
                        "Names, areas, addresses and phones (some missing). No coordinates in the source, so these "
                                + "are not returned by /nearby."),
                new DatasetSourceResponse("Women help centres (selected)", "women_help_centres_selected.csv", true,
                        helpLocationRepository.countByType(HelpLocationType.WOMEN_HELP),
                        "/api/help-locations?type=WOMEN_HELP",
                        "NGO/organisation directory with support type, sectors and tier. No coordinates or phone "
                                + "numbers in the source. District is set to Hyderabad only when the address mentions "
                                + "Hyderabad or Secunderabad."),
                new DatasetSourceResponse("Crimes against women by category", "women_crime_categories.csv", true,
                        crimeRepository.count(), "/api/safety-data/women-crime-stats",
                        "Annual state-level counts for Andhra Pradesh (2011-2013) and Telangana (2011-2021)."),
                new DatasetSourceResponse("Andhra Pradesh 2002-2013 (IT Act / IPC)", "SafeWalk_Andhra_Pradesh_2002_2013.csv",
                        false, 0, null,
                        "Not imported: the columns do not state what the counts measure and several values appear "
                                + "shifted between columns, so they cannot be presented accurately."),
                new DatasetSourceResponse("Internet research compilation", "SafeWalk_Internet_Research_Datasets.pdf",
                        false, 0, null,
                        "Not imported: PDF reference document (e.g. GHMC waterlogging places without coordinates)."),
                new DatasetSourceResponse("Telangana combined district data", "SafeWalk_Telangana_Combined_Data.pdf",
                        false, 0, null,
                        "Not imported: PDF tables (district-wise records such as missing persons); needs the "
                                + "original CSV files for reliable parsing."),
                new DatasetSourceResponse("Hyderabad City Police Annual Report 2024", "HYDANNUAL REPORT2024.pdf",
                        false, 0, null, "Not imported: narrative PDF report, not a structured dataset."));
    }

    private static double median(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        List<Integer> sorted = values.stream().sorted().toList();
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}

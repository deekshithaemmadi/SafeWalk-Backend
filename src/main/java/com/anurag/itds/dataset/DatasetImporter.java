package com.anurag.itds.dataset;

import com.anurag.itds.entity.HelpLocation;
import com.anurag.itds.entity.HelpLocationType;
import com.anurag.itds.entity.WomenCrimeStatistic;
import com.anurag.itds.repository.HelpLocationRepository;
import com.anurag.itds.repository.WomenCrimeStatisticRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Imports the bundled static SafeWalk datasets (classpath {@code datasets/}) into the database.
 * Idempotent upsert keyed on a deterministic {@code source_key}; never deletes rows. Values are
 * taken from the files as-is: missing coordinates/phones stay null and nothing is fabricated.
 */
@Service
public class DatasetImporter {

    private static final Logger log = LoggerFactory.getLogger(DatasetImporter.class);

    public static final String HOSPITALS_FILE = "datasets/hyderabad_hospitals.csv";
    public static final String POLICE_FILE = "datasets/hyderabad_police_stations.csv";
    public static final String WOMEN_HELP_FILE = "datasets/women_help_centres_selected.csv";
    public static final String CRIME_FILE = "datasets/women_crime_categories.csv";

    public static final String POLICE_SOURCE = "Hyderabad Police Stations dataset (hyderabad_police_stations.csv)";
    public static final String WOMEN_HELP_SOURCE = "Women Help Centres (selected) dataset (women_help_centres_selected.csv)";
    public static final String CRIME_SOURCE = "State-level crimes against women, annual (women_crime_categories.csv)";

    /** Rough Telangana bounding box; coordinates outside it are treated as invalid. */
    static final double MIN_LAT = 15.5, MAX_LAT = 20.0, MIN_LON = 77.0, MAX_LON = 81.5;
    /** Same-name facilities from one source closer than this are treated as duplicates. */
    static final double DUPLICATE_DISTANCE_KM = 0.05;

    /** Source CSV header (typos preserved) -> normalized category code. */
    public static final Map<String, String> CRIME_CATEGORIES = new LinkedHashMap<>();

    static {
        CRIME_CATEGORIES.put("RAPE", "RAPE");
        CRIME_CATEGORIES.put("KIDNAP & ABDUCTION", "KIDNAPPING_AND_ABDUCTION");
        CRIME_CATEGORIES.put("DOWRY DEATHS", "DOWRY_DEATHS");
        CRIME_CATEGORIES.put("CRUETLY BY HUSBAND OR HIS RELATIVES", "CRUELTY_BY_HUSBAND_OR_RELATIVES");
        CRIME_CATEGORIES.put("HUMAN TRAFFICKING", "HUMAN_TRAFFICKING");
        CRIME_CATEGORIES.put("ASSAULT ON MODESTY", "ASSAULT_ON_MODESTY");
        CRIME_CATEGORIES.put("SEXUAL HARRASSMENT", "SEXUAL_HARASSMENT");
        CRIME_CATEGORIES.put("IMORTAL TRAFFICKING", "IMMORAL_TRAFFIC");
        CRIME_CATEGORIES.put("TOTAL CRIMES AGAINST WOMEN", "TOTAL");
    }

    private final HelpLocationRepository helpLocationRepository;
    private final WomenCrimeStatisticRepository crimeRepository;

    public DatasetImporter(HelpLocationRepository helpLocationRepository,
                           WomenCrimeStatisticRepository crimeRepository) {
        this.helpLocationRepository = helpLocationRepository;
        this.crimeRepository = crimeRepository;
    }

    public static final class Stats {
        public final String dataset;
        public int read, inserted, updated, unchanged, duplicates, skipped, invalidCoordinates;

        Stats(String dataset) {
            this.dataset = dataset;
        }

        @Override
        public String toString() {
            return "%s: read=%d inserted=%d updated=%d unchanged=%d duplicates=%d skipped=%d invalidCoordinates=%d"
                    .formatted(dataset, read, inserted, updated, unchanged, duplicates, skipped, invalidCoordinates);
        }
    }

    @Transactional
    public List<Stats> importAll() {
        List<Stats> all = new ArrayList<>();
        Map<String, HelpLocation> existing = new HashMap<>();
        helpLocationRepository.findAll().forEach(h -> existing.put(h.getSourceKey(), h));
        all.add(importHospitals(read(HOSPITALS_FILE), existing));
        all.add(importPolice(read(POLICE_FILE), existing));
        all.add(importWomenHelp(read(WOMEN_HELP_FILE), existing));
        all.add(importCrimeStats(read(CRIME_FILE)));
        all.forEach(s -> log.info("Dataset import {}", s));
        return all;
    }

    Stats importHospitals(List<Map<String, String>> rows, Map<String, HelpLocation> existing) {
        Stats stats = new Stats("hospitals");
        Map<String, List<double[]>> seenByName = new HashMap<>();
        Set<String> seenKeys = new HashSet<>();
        for (Map<String, String> row : rows) {
            stats.read++;
            String name = clean(row.get("facility_name"));
            String source = clean(row.get("source"));
            if (name == null || source == null) {
                stats.skipped++;
                continue;
            }
            Double lat = parseDouble(row.get("latitude"));
            Double lon = parseDouble(row.get("longitude"));
            if (!validCoordinates(lat, lon)) {
                if (lat != null || lon != null) {
                    stats.invalidCoordinates++;
                }
                lat = null;
                lon = null;
            }
            String nameKey = normalize(source) + "|" + normalize(name);
            if (lat != null && isNearDuplicate(seenByName.computeIfAbsent(nameKey, k -> new ArrayList<>()), lat, lon)) {
                stats.duplicates++;
                continue;
            }
            String key = key(HelpLocationType.HOSPITAL, nameKey + "|" + coordKey(lat) + "|" + coordKey(lon)
                    + (lat == null ? "|" + normalize(row.get("address")) : ""));
            if (!seenKeys.add(key)) {
                stats.duplicates++;
                continue;
            }
            if (lat != null) {
                seenByName.get(nameKey).add(new double[]{lat, lon});
            }
            String locality = firstNonBlank(row.get("area_or_ward"), row.get("zone"));
            upsert(existing, stats, HelpLocationType.HOSPITAL, name, key, h -> {
                h.setCategory(clean(row.get("facility_type")));
                h.setDistrict(clean(row.get("district")));
                h.setLocality(locality);
                h.setAddress(clean(row.get("address")));
                return null;
            }, lat, lon, null, null, null, clean(row.get("source_year")), source);
        }
        return stats;
    }

    Stats importPolice(List<Map<String, String>> rows, Map<String, HelpLocation> existing) {
        Stats stats = new Stats("police stations");
        Set<String> seenKeys = new HashSet<>();
        for (Map<String, String> row : rows) {
            stats.read++;
            String name = clean(row.get("Station Name"));
            if (name == null) {
                stats.skipped++;
                continue;
            }
            String address = clean(row.get("Address"));
            String key = key(HelpLocationType.POLICE, normalize(name) + "|" + normalize(address));
            if (!seenKeys.add(key)) {
                stats.duplicates++;
                continue;
            }
            String phone = digitsOrNull(row.get("Phone"));
            String locality = clean(row.get("Area"));
            upsert(existing, stats, HelpLocationType.POLICE, name, key, h -> {
                h.setCategory("Police Station");
                h.setDistrict("Hyderabad");
                h.setLocality(locality);
                h.setAddress(address);
                return null;
            }, null, null, phone, null, null, null, POLICE_SOURCE);
        }
        return stats;
    }

    Stats importWomenHelp(List<Map<String, String>> rows, Map<String, HelpLocation> existing) {
        Stats stats = new Stats("women help centres");
        Set<String> seenKeys = new HashSet<>();
        for (Map<String, String> row : rows) {
            stats.read++;
            String name = clean(row.get("organization_name"));
            if (name == null) {
                stats.skipped++;
                continue;
            }
            String address = clean(row.get("address"));
            String key = key(HelpLocationType.WOMEN_HELP, normalize(name) + "|" + normalize(address));
            if (!seenKeys.add(key)) {
                stats.duplicates++;
                continue;
            }
            String lowerAddress = address == null ? "" : address.toLowerCase(Locale.ROOT);
            String district = lowerAddress.contains("hyderabad") || lowerAddress.contains("secunderabad")
                    ? "Hyderabad" : null;
            upsert(existing, stats, HelpLocationType.WOMEN_HELP, name, key, h -> {
                h.setCategory(clean(row.get("support_type")));
                h.setDistrict(district);
                h.setLocality(null);
                h.setAddress(address);
                return null;
            }, null, null, null, clean(row.get("sectors")), clean(row.get("tier")), null, WOMEN_HELP_SOURCE);
        }
        return stats;
    }

    Stats importCrimeStats(List<Map<String, String>> rows) {
        Stats stats = new Stats("women crime statistics");
        Map<String, WomenCrimeStatistic> existing = new HashMap<>();
        crimeRepository.findAll().forEach(s -> existing.put(s.getSourceKey(), s));
        Set<String> seenKeys = new HashSet<>();
        for (Map<String, String> row : rows) {
            String state = clean(row.get("STATES"));
            Integer year = parseInt(row.get("YEAR"));
            if (state == null || year == null) {
                stats.read++;
                stats.skipped++;
                continue;
            }
            Map<String, Integer> counts = new LinkedHashMap<>();
            boolean allZero = true;
            for (Map.Entry<String, String> c : CRIME_CATEGORIES.entrySet()) {
                Integer value = parseInt(row.get(c.getKey()));
                counts.put(c.getValue(), value);
                if (value != null && value != 0) {
                    allZero = false;
                }
            }
            for (Map.Entry<String, Integer> c : counts.entrySet()) {
                stats.read++;
                String key = "WCS:" + normalize(state) + ":" + year + ":" + c.getKey();
                if (!seenKeys.add(key)) {
                    stats.duplicates++;
                    continue;
                }
                // An all-zero row means the state was not reported separately (e.g. Telangana pre-2014), not zero crime.
                Integer count = allZero ? null : c.getValue();
                String region = clean(row.get("REGION_CONTEXT"));
                WomenCrimeStatistic stat = existing.get(key);
                if (stat == null) {
                    stat = new WomenCrimeStatistic(state, year, c.getKey(), key);
                    stats.inserted++;
                } else if (Objects.equals(stat.getCaseCount(), count) && Objects.equals(stat.getRegionContext(), region)
                        && CRIME_SOURCE.equals(stat.getSource())) {
                    stats.unchanged++;
                    continue;
                } else {
                    stats.updated++;
                }
                stat.setCaseCount(count);
                stat.setRegionContext(region);
                stat.setSource(CRIME_SOURCE);
                crimeRepository.save(stat);
            }
        }
        return stats;
    }

    private void upsert(Map<String, HelpLocation> existing, Stats stats, HelpLocationType type, String name, String key,
                        Function<HelpLocation, Void> fields, Double lat, Double lon, String phone,
                        String sectors, String tier, String sourceYear, String source) {
        HelpLocation h = existing.get(key);
        boolean isNew = h == null;
        if (isNew) {
            h = new HelpLocation(type, name, key);
        }
        String before = isNew ? null : fingerprint(h);
        h.setType(type);
        h.setName(name);
        fields.apply(h);
        h.setLatitude(lat);
        h.setLongitude(lon);
        h.setPhone(phone);
        h.setSupportSectors(sectors);
        h.setTier(tier);
        h.setSourceYear(sourceYear);
        h.setSource(source);
        if (isNew) {
            stats.inserted++;
        } else if (before.equals(fingerprint(h))) {
            stats.unchanged++;
            return;
        } else {
            stats.updated++;
        }
        existing.put(key, helpLocationRepository.save(h));
    }

    private static String fingerprint(HelpLocation h) {
        return String.join("\u0001", String.valueOf(h.getType()), String.valueOf(h.getName()),
                String.valueOf(h.getCategory()), String.valueOf(h.getDistrict()), String.valueOf(h.getLocality()),
                String.valueOf(h.getAddress()), String.valueOf(h.getLatitude()), String.valueOf(h.getLongitude()),
                String.valueOf(h.getPhone()), String.valueOf(h.getSupportSectors()), String.valueOf(h.getTier()),
                String.valueOf(h.getSourceYear()), String.valueOf(h.getSource()));
    }

    private static List<Map<String, String>> read(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return CsvParser.parseWithHeader(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read bundled dataset " + path, e);
        }
    }

    static boolean validCoordinates(Double lat, Double lon) {
        return lat != null && lon != null && lat >= MIN_LAT && lat <= MAX_LAT && lon >= MIN_LON && lon <= MAX_LON;
    }

    private static boolean isNearDuplicate(List<double[]> seen, double lat, double lon) {
        for (double[] p : seen) {
            if (haversineKm(p[0], p[1], lat, lon) < DUPLICATE_DISTANCE_KM) {
                return true;
            }
        }
        return false;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String v = value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        return v.isEmpty() ? null : v;
    }

    static String normalize(String value) {
        String v = clean(value);
        return v == null ? "" : v.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String firstNonBlank(String a, String b) {
        String ca = clean(a);
        return ca != null ? ca : clean(b);
    }

    private static String digitsOrNull(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    private static Double parseDouble(String value) {
        String v = clean(value);
        if (v == null) {
            return null;
        }
        try {
            double d = Double.parseDouble(v);
            return Double.isFinite(d) ? d : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String value) {
        String v = clean(value);
        if (v == null) {
            return null;
        }
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String coordKey(Double value) {
        return value == null ? "-" : String.format(Locale.ROOT, "%.5f", value);
    }

    private static String key(HelpLocationType type, String material) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            return type.name() + ":" + HexFormat.of().formatHex(hash, 0, 20);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

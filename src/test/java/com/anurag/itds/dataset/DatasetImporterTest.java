package com.anurag.itds.dataset;

import com.anurag.itds.dataset.DatasetImporter.Stats;
import com.anurag.itds.entity.HelpLocation;
import com.anurag.itds.entity.HelpLocationType;
import com.anurag.itds.entity.WomenCrimeStatistic;
import com.anurag.itds.repository.HelpLocationRepository;
import com.anurag.itds.repository.WomenCrimeStatisticRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Imports the real bundled datasets into the isolated H2 database (rolled back after each test). */
@SpringBootTest
@Transactional
class DatasetImporterTest {

    @Autowired
    DatasetImporter importer;

    @Autowired
    HelpLocationRepository helpLocations;

    @Autowired
    WomenCrimeStatisticRepository crimeStats;

    private static Stats find(List<Stats> stats, String dataset) {
        return stats.stream().filter(s -> s.dataset.equals(dataset)).findFirst().orElseThrow();
    }

    @Test
    void importsAllBundledDatasetsWithConsistentCounts() {
        List<Stats> stats = importer.importAll();

        Stats hospitals = find(stats, "hospitals");
        assertThat(hospitals.read).isEqualTo(1283);
        assertThat(hospitals.inserted + hospitals.duplicates + hospitals.skipped).isEqualTo(hospitals.read);
        assertThat(hospitals.inserted).isGreaterThan(1200);
        assertThat(hospitals.invalidCoordinates).isZero();
        assertThat(helpLocations.countByType(HelpLocationType.HOSPITAL)).isEqualTo(hospitals.inserted);

        Stats police = find(stats, "police stations");
        assertThat(police.read).isEqualTo(79);
        assertThat(police.inserted + police.duplicates).isEqualTo(79);
        assertThat(helpLocations.countByType(HelpLocationType.POLICE)).isEqualTo(police.inserted);

        Stats help = find(stats, "women help centres");
        assertThat(help.read).isEqualTo(169);
        assertThat(help.inserted).isEqualTo(169);
        assertThat(helpLocations.countByType(HelpLocationType.WOMEN_HELP)).isEqualTo(169);

        Stats crime = find(stats, "women crime statistics");
        assertThat(crime.read).isEqualTo(14 * 9);
        assertThat(crime.inserted).isEqualTo(14 * 9);
        assertThat(crimeStats.count()).isEqualTo(14 * 9);
    }

    @Test
    void importedValuesComeFromTheSourceWithoutFabrication() {
        importer.importAll();
        List<HelpLocation> all = helpLocations.findAll();

        assertThat(all).filteredOn(h -> h.getType() == HelpLocationType.POLICE)
                .allSatisfy(h -> {
                    // The police stations CSV contains valid coordinates within the Telangana bounding box.
                    assertThat(h.getLatitude()).isBetween(15.5, 20.0);
                    assertThat(h.getLongitude()).isBetween(77.0, 81.5);
                    assertThat(h.getSource()).isEqualTo(DatasetImporter.POLICE_SOURCE);
                    if (h.getPhone() != null) {
                        assertThat(h.getPhone()).matches("\\d+");
                    }
                })
                .anySatisfy(h -> assertThat(h.getPhone()).isNull());
        assertThat(all).filteredOn(h -> h.getType() == HelpLocationType.WOMEN_HELP)
                .allSatisfy(h -> {
                    // The women help centres CSV contains valid coordinates within the Telangana bounding box.
                    assertThat(h.getLatitude()).isBetween(15.5, 20.0);
                    assertThat(h.getLongitude()).isBetween(77.0, 81.5);
                    assertThat(h.getPhone()).isNull();
                })
                .anySatisfy(h -> assertThat(h.getSupportSectors()).hasSizeGreaterThan(500));
        assertThat(all).filteredOn(h -> h.getType() == HelpLocationType.HOSPITAL)
                .allSatisfy(h -> {
                    assertThat(h.getLatitude()).isBetween(15.5, 20.0);
                    assertThat(h.getSourceYear()).isIn("2018", "2025");
                });
        assertThat(all).extracting(HelpLocation::getName).contains("SHIVA NARSING HOME", "Amberpet Police Station", "Safa");

        Map<String, WomenCrimeStatistic> crime = new HashMap<>();
        crimeStats.findAll().forEach(s -> crime.put(s.getState() + s.getYear() + s.getCategory(), s));
        assertThat(crime.get("Telangana2011TOTAL").getCaseCount()).as("not reported separately").isNull();
        assertThat(crime.get("Telangana2014TOTAL").getCaseCount()).isEqualTo(14136);
        assertThat(crime.get("Andhra Pradesh2013RAPE").getCaseCount()).isEqualTo(1635);
        assertThat(crime.get("Telangana2016HUMAN_TRAFFICKING").getCaseCount()).isEqualTo(9);
    }

    @Test
    void reimportIsIdempotentAndUpdatesChangedRecords() {
        List<Stats> first = importer.importAll();
        long locations = helpLocations.count();

        List<Stats> second = importer.importAll();
        for (int i = 0; i < first.size(); i++) {
            assertThat(second.get(i).inserted).isZero();
            assertThat(second.get(i).updated).isZero();
            assertThat(second.get(i).unchanged).isEqualTo(first.get(i).inserted);
        }
        assertThat(helpLocations.count()).isEqualTo(locations);

        HelpLocation edited = helpLocations.findAll().get(0);
        edited.setCategory("edited by hand");
        helpLocations.save(edited);
        long userRecord = helpLocations.save(new HelpLocation(HelpLocationType.POLICE, "Manual entry", "manual:1")).getId();

        List<Stats> third = importer.importAll();
        assertThat(third.stream().mapToInt(s -> s.updated).sum()).isEqualTo(1);
        assertThat(helpLocations.findById(edited.getId()).orElseThrow().getCategory()).isNotEqualTo("edited by hand");
        assertThat(helpLocations.findById(userRecord)).as("import never deletes rows").isPresent();
    }

    @Test
    void hospitalRowsAreValidatedAndDeduplicated() {
        List<Map<String, String>> rows = List.of(
                hospital("City Clinic", "17.40000", "78.50000"),
                hospital("CITY  clinic", "17.40010", "78.50010"),
                hospital("City Clinic", "17.41000", "78.50000"),
                hospital("Far Away", "40.0", "78.5"),
                hospital("Bad Number", "abc", "78.5"),
                hospital(" ", "17.4", "78.5"));

        Stats stats = importer.importHospitals(rows, new HashMap<>());

        assertThat(stats.read).isEqualTo(6);
        assertThat(stats.duplicates).as("same name within 50 m").isEqualTo(1);
        assertThat(stats.skipped).as("blank name").isEqualTo(1);
        assertThat(stats.inserted).isEqualTo(4);
        assertThat(stats.invalidCoordinates).isEqualTo(2);
        assertThat(helpLocations.findAll()).filteredOn(h -> h.getName().equals("Far Away"))
                .singleElement().satisfies(h -> assertThat(h.getLatitude()).isNull());
    }

    private static Map<String, String> hospital(String name, String lat, String lon) {
        Map<String, String> row = new HashMap<>();
        row.put("facility_name", name);
        row.put("facility_type", "HOSPITALS");
        row.put("district", "Hyderabad");
        row.put("latitude", lat);
        row.put("longitude", lon);
        row.put("source_year", "2018");
        row.put("source", "Test source");
        row.put("address", name);
        return row;
    }
}

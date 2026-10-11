package com.anurag.itds.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One annual, state-level count of a crimes-against-women category, imported from a
 * static dataset. {@code caseCount} is null when the source did not report the
 * state separately (e.g. Telangana before 2014). Not live and not location-specific.
 */
@Entity
@Table(name = "women_crime_statistics")
public class WomenCrimeStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state", nullable = false, length = 80)
    private String state;

    @Column(name = "region_context", length = 160)
    private String regionContext;

    @Column(name = "stat_year", nullable = false)
    private Integer year;

    @Column(name = "category", nullable = false, length = 60)
    private String category;

    @Column(name = "case_count")
    private Integer caseCount;

    @Column(name = "source", nullable = false, length = 255)
    private String source;

    @Column(name = "source_key", nullable = false, length = 200, unique = true)
    private String sourceKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WomenCrimeStatistic() {
    }

    public WomenCrimeStatistic(String state, Integer year, String category, String sourceKey) {
        this.state = state;
        this.year = year;
        this.category = category;
        this.sourceKey = sourceKey;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getState() { return state; }
    public String getRegionContext() { return regionContext; }
    public void setRegionContext(String regionContext) { this.regionContext = regionContext; }
    public Integer getYear() { return year; }
    public String getCategory() { return category; }
    public Integer getCaseCount() { return caseCount; }
    public void setCaseCount(Integer caseCount) { this.caseCount = caseCount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceKey() { return sourceKey; }
}

package com.anurag.itds.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A help location imported from one of the SafeWalk source datasets
 * (hospitals, police stations, women's help centres).
 *
 * <p><strong>Data honesty:</strong> only the hospital dataset contains real
 * latitude/longitude values. Police stations and women's help centres do NOT
 * have coordinates in the source data, so their {@code latitude}/{@code longitude}
 * are left {@code null} and are never fabricated.</p>
 *
 * <p>A {@code source_key} unique column stores a deterministic natural key so the
 * CSV import is idempotent (re-running the import updates instead of duplicating).</p>
 */
@Entity
@Table(
    name = "help_locations",
    indexes = {
        @Index(name = "idx_help_locations_type", columnList = "type"),
        @Index(name = "idx_help_locations_district", columnList = "district"),
        @Index(name = "idx_help_locations_locality", columnList = "locality")
    }
)
public class HelpLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private HelpLocationType type;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** facility_type / support_type / "POLICE STATION". */
    @Column(name = "category", length = 160)
    private String category;

    @Column(name = "district", length = 120)
    private String district;

    /** zone / area_or_ward / Area — the sub-district locality. */
    @Column(name = "locality", length = 190)
    private String locality;

    @Column(name = "address", length = 500)
    private String address;

    /** Only hospitals carry coordinates; otherwise null (never fabricated). */
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "phone", length = 40)
    private String phone;

    /** Women's help centres: support sectors. */
    @Column(name = "support_sectors", length = 1000)
    private String supportSectors;

    /** Women's help centres: tier label. */
    @Column(name = "tier", length = 80)
    private String tier;

    @Column(name = "source_year", length = 20)
    private String sourceYear;

    @Column(name = "source", length = 255)
    private String source;

    /** Deterministic natural key used to make CSV import idempotent. */
    @Column(name = "source_key", nullable = false, length = 400, unique = true)
    private String sourceKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HelpLocation() {
        // for JPA
    }

    /**
     * @param sourceKey stable natural key derived from the source record, used to
     *                  deduplicate re-imports (unique in the database)
     */
    public HelpLocation(HelpLocationType type, String name, String sourceKey) {
        this.type = type;
        this.name = name;
        this.sourceKey = sourceKey;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public HelpLocationType getType() {
        return type;
    }

    public void setType(HelpLocationType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSupportSectors() {
        return supportSectors;
    }

    public void setSupportSectors(String supportSectors) {
        this.supportSectors = supportSectors;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getSourceYear() {
        return sourceYear;
    }

    public void setSourceYear(String sourceYear) {
        this.sourceYear = sourceYear;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

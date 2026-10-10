package com.anurag.itds.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A record that a user activated SOS.
 *
 * <p><strong>Data honesty:</strong> SafeWalk does NOT automatically dispatch, SMS,
 * call, or broadcast alerts. This entity only stores that the user pressed SOS and
 * — solely if they consented — the single location snapshot they chose to share.
 * The service layer must never claim an alert was delivered.</p>
 */
@Entity
@Table(
    name = "sos_events",
    indexes = {
        @Index(name = "idx_sos_events_user", columnList = "user_id")
    }
)
public class SosEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "message", length = 500)
    private String message;

    /** Whether the user explicitly consented to share their location. */
    @Column(name = "share_location", nullable = false)
    private boolean shareLocation;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "accuracy")
    private Double accuracy;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SosEvent() {
        // for JPA
    }

    public SosEvent(User user) {
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isShareLocation() {
        return shareLocation;
    }

    public void setShareLocation(boolean shareLocation) {
        this.shareLocation = shareLocation;
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

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

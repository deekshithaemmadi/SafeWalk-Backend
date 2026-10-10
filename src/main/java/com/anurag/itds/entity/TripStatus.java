package com.anurag.itds.entity;

/** Lifecycle state of a {@link Trip}. Stored as {@link #name()} in the database. */
public enum TripStatus {
    PLANNED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

package com.anurag.itds.entity;

/**
 * Moderation state of a {@link CommunityReport}.
 *
 * <ul>
 *   <li>{@code PENDING} — freshly submitted, not yet reviewed (default).</li>
 *   <li>{@code VERIFIED} — reviewed and confirmed by a moderator.</li>
 *   <li>{@code REJECTED} — reviewed and rejected by a moderator.</li>
 * </ul>
 * Stored as {@link #name()} in the database.
 */
public enum ReportStatus {
    PENDING,
    VERIFIED,
    REJECTED
}

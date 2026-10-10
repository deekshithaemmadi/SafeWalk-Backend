package com.anurag.itds.service;

import com.anurag.itds.dto.CommunityReportResponse;
import com.anurag.itds.dto.EmergencyContactResponse;
import com.anurag.itds.dto.HelpLocationResponse;
import com.anurag.itds.dto.TripResponse;
import com.anurag.itds.dto.UserResponse;
import com.anurag.itds.entity.CommunityReport;
import com.anurag.itds.entity.EmergencyContact;
import com.anurag.itds.entity.HelpLocation;
import com.anurag.itds.entity.Trip;
import com.anurag.itds.entity.User;

import java.util.Collections;
import java.util.List;

/**
 * Small, dependency-free helpers to convert entities into API DTOs, keeping the
 * password/hash strictly out of responses.
 */
public final class Dtos {

    private Dtos() {
    }

    public static EmergencyContactResponse toContactResponse(EmergencyContact c) {
        return new EmergencyContactResponse(c.getId(), c.getName(), c.getPhone(), c.getRelation());
    }

    public static List<EmergencyContactResponse> toContactResponses(List<EmergencyContact> contacts) {
        if (contacts == null) {
            return Collections.emptyList();
        }
        return contacts.stream().map(Dtos::toContactResponse).toList();
    }

    /** Builds a {@link UserResponse} from a user + its contacts (no password). */
    public static UserResponse toUserResponse(User user, List<EmergencyContact> contacts) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                toContactResponses(contacts)
        );
    }

    public static TripResponse toTripResponse(Trip t) {
        return new TripResponse(
                t.getId(),
                t.getStatus(),
                t.getOriginLabel(),
                t.getOriginLatitude(),
                t.getOriginLongitude(),
                t.getDestinationLabel(),
                t.getDestinationLatitude(),
                t.getDestinationLongitude(),
                t.getLastLatitude(),
                t.getLastLongitude(),
                t.getStartedAt(),
                t.getEndedAt(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    public static HelpLocationResponse toHelpLocationResponse(HelpLocation h) {
        boolean hasCoords = h.getLatitude() != null && h.getLongitude() != null;
        return new HelpLocationResponse(
                h.getId(),
                h.getType(),
                h.getName(),
                h.getCategory(),
                h.getDistrict(),
                h.getLocality(),
                h.getAddress(),
                h.getLatitude(),
                h.getLongitude(),
                h.getPhone(),
                h.getSupportSectors(),
                h.getTier(),
                h.getSourceYear(),
                h.getSource(),
                hasCoords
        );
    }

    public static CommunityReportResponse toReportResponse(CommunityReport r) {
        return new CommunityReportResponse(
                r.getId(),
                r.getUser() == null ? null : r.getUser().getId(),
                r.getCategory(),
                r.getDescription(),
                r.getLocation(),
                r.getLatitude(),
                r.getLongitude(),
                r.getStatus(),
                r.getModerationNote(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}

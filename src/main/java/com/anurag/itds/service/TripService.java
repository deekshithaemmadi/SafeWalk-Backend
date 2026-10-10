package com.anurag.itds.service;

import com.anurag.itds.dto.CreateTripRequest;
import com.anurag.itds.dto.TripLocationRequest;
import com.anurag.itds.dto.TripResponse;
import com.anurag.itds.entity.Trip;
import com.anurag.itds.entity.TripStatus;
import com.anurag.itds.entity.User;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.exception.ResourceNotFoundException;
import com.anurag.itds.repository.TripRepository;
import com.anurag.itds.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Trip lifecycle for the authenticated user.
 *
 * <p>Status machine: {@code PLANNED -> ACTIVE -> COMPLETED}, with
 * {@code CANCELLED} reachable from {@code PLANNED} or {@code ACTIVE}. Location
 * updates are accepted only while a trip is {@code ACTIVE}. All operations are
 * scoped to the caller's user id — a user can only ever see or mutate their own
 * trips (the repository queries are all user-scoped).</p>
 */
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public TripService(TripRepository tripRepository, UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TripResponse create(Long userId, CreateTripRequest request) {
        User user = requireUser(userId);
        Trip trip = new Trip(user);
        applyPlannedFields(trip, request);
        return Dtos.toTripResponse(tripRepository.save(trip));
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> listOwn(Long userId, Pageable pageable) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(Dtos::toTripResponse);
    }

    @Transactional(readOnly = true)
    public TripResponse getOwn(Long userId, Long tripId) {
        return Dtos.toTripResponse(requireOwnedTrip(userId, tripId));
    }

    @Transactional
    public TripResponse start(Long userId, Long tripId) {
        Trip trip = requireOwnedTrip(userId, tripId);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new BadRequestException(
                    "Only a PLANNED trip can be started. Current status: " + trip.getStatus());
        }
        trip.setStatus(TripStatus.ACTIVE);
        trip.setStartedAt(Instant.now());
        return Dtos.toTripResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse end(Long userId, Long tripId) {
        Trip trip = requireOwnedTrip(userId, tripId);
        if (trip.getStatus() != TripStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only an ACTIVE trip can be ended. Current status: " + trip.getStatus());
        }
        trip.setStatus(TripStatus.COMPLETED);
        trip.setEndedAt(Instant.now());
        return Dtos.toTripResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse cancel(Long userId, Long tripId) {
        Trip trip = requireOwnedTrip(userId, tripId);
        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new BadRequestException(
                    "A " + trip.getStatus() + " trip cannot be cancelled.");
        }
        trip.setStatus(TripStatus.CANCELLED);
        return Dtos.toTripResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse updateLocation(Long userId, Long tripId, TripLocationRequest request) {
        Trip trip = requireOwnedTrip(userId, tripId);
        if (trip.getStatus() != TripStatus.ACTIVE) {
            throw new BadRequestException(
                    "Location can only be shared while a trip is ACTIVE. Current status: " + trip.getStatus());
        }
        trip.setLastLatitude(request.latitude());
        trip.setLastLongitude(request.longitude());
        return Dtos.toTripResponse(tripRepository.save(trip));
    }

    private void applyPlannedFields(Trip trip, CreateTripRequest request) {
        trip.setOriginLabel(request.originLabel());
        if (request.origin() != null) {
            trip.setOriginLatitude(request.origin().latitude());
            trip.setOriginLongitude(request.origin().longitude());
        }
        trip.setDestinationLabel(request.destinationLabel());
        if (request.destination() != null) {
            trip.setDestinationLatitude(request.destination().latitude());
            trip.setDestinationLongitude(request.destination().longitude());
        }
    }

    private Trip requireOwnedTrip(Long userId, Long tripId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found."));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}

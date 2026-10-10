package com.anurag.itds.controller;

import com.anurag.itds.dto.CreateTripRequest;
import com.anurag.itds.dto.TripLocationRequest;
import com.anurag.itds.dto.TripResponse;
import com.anurag.itds.security.CurrentUserProvider;
import com.anurag.itds.service.TripService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trip creation and lifecycle for the authenticated user. Ownership is enforced
 * on every operation (user id from the JWT principal + user-scoped queries).
 */
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final CurrentUserProvider currentUserProvider;

    public TripController(TripService tripService, CurrentUserProvider currentUserProvider) {
        this.tripService = tripService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse create(@Valid @RequestBody CreateTripRequest request) {
        return tripService.create(currentUserProvider.requireCurrentUserId(), request);
    }

    @GetMapping
    public Page<TripResponse> list(Pageable pageable) {
        return tripService.listOwn(currentUserProvider.requireCurrentUserId(), pageable);
    }

    @GetMapping("/{id}")
    public TripResponse get(@PathVariable Long id) {
        return tripService.getOwn(currentUserProvider.requireCurrentUserId(), id);
    }

    @PostMapping("/{id}/start")
    public TripResponse start(@PathVariable Long id) {
        return tripService.start(currentUserProvider.requireCurrentUserId(), id);
    }

    @PostMapping("/{id}/end")
    public TripResponse end(@PathVariable Long id) {
        return tripService.end(currentUserProvider.requireCurrentUserId(), id);
    }

    @PostMapping("/{id}/cancel")
    public TripResponse cancel(@PathVariable Long id) {
        return tripService.cancel(currentUserProvider.requireCurrentUserId(), id);
    }

    @PostMapping("/{id}/location")
    public TripResponse updateLocation(@PathVariable Long id,
                                       @Valid @RequestBody TripLocationRequest request) {
        return tripService.updateLocation(currentUserProvider.requireCurrentUserId(), id, request);
    }
}

package com.anurag.itds.controller;

import com.anurag.itds.dto.SosRequest;
import com.anurag.itds.dto.SosResponse;
import com.anurag.itds.security.CurrentUserProvider;
import com.anurag.itds.service.SosService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SOS activation for the authenticated user. Creating an event only RECORDS it;
 * nothing is dispatched. History is visible only to its owner.
 */
@RestController
@RequestMapping("/api/sos")
public class SosController {

    private final SosService sosService;
    private final CurrentUserProvider currentUserProvider;

    public SosController(SosService sosService, CurrentUserProvider currentUserProvider) {
        this.sosService = sosService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SosResponse create(@Valid @RequestBody SosRequest request) {
        return sosService.create(currentUserProvider.requireCurrentUserId(), request);
    }

    @GetMapping
    public List<SosResponse> history() {
        return sosService.listOwn(currentUserProvider.requireCurrentUserId());
    }
}

package com.anurag.itds.controller;

import com.anurag.itds.dto.EmergencyContactRequest;
import com.anurag.itds.dto.EmergencyContactResponse;
import com.anurag.itds.security.CurrentUserProvider;
import com.anurag.itds.service.EmergencyContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Emergency-contact management for the authenticated user. The user id always
 * comes from the JWT principal; every query is additionally scoped by user id
 * in the service for defense in depth.
 */
@RestController
@RequestMapping("/api/emergency-contacts")
public class EmergencyContactController {

    private final EmergencyContactService contactService;
    private final CurrentUserProvider currentUserProvider;

    public EmergencyContactController(EmergencyContactService contactService,
                                      CurrentUserProvider currentUserProvider) {
        this.contactService = contactService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<EmergencyContactResponse> list() {
        return contactService.listOwn(currentUserProvider.requireCurrentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyContactResponse create(@Valid @RequestBody EmergencyContactRequest request) {
        return contactService.createOwn(currentUserProvider.requireCurrentUserId(), request);
    }

    @PutMapping("/{id}")
    public EmergencyContactResponse update(@PathVariable Long id,
                                           @Valid @RequestBody EmergencyContactRequest request) {
        return contactService.updateOwn(currentUserProvider.requireCurrentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        contactService.deleteOwn(currentUserProvider.requireCurrentUserId(), id);
    }
}

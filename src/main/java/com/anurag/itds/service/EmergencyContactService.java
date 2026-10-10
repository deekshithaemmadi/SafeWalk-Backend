package com.anurag.itds.service;

import com.anurag.itds.dto.EmergencyContactRequest;
import com.anurag.itds.dto.EmergencyContactResponse;
import com.anurag.itds.entity.EmergencyContact;
import com.anurag.itds.entity.User;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.exception.ConflictException;
import com.anurag.itds.exception.ForbiddenException;
import com.anurag.itds.exception.ResourceNotFoundException;
import com.anurag.itds.repository.EmergencyContactRepository;
import com.anurag.itds.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD for the authenticated user's own emergency contacts.
 *
 * <ul>
 *   <li>Every operation is scoped to the caller's user id (derived from the JWT
 *       principal at the controller) — never from a client-supplied user id.</li>
 *   <li>A contact phone may not be duplicated within one user's contact list.</li>
 *   <li>The project's minimum of two emergency contacts is enforced on the
 *       workflow where it applies: a user may not delete a contact while doing
 *       so would leave them with fewer than two contacts.</li>
 * </ul>
 */
@Service
public class EmergencyContactService {

    /** Minimum number of emergency contacts a user is expected to keep. */
    public static final int MIN_CONTACTS = 2;

    private final EmergencyContactRepository contactRepository;
    private final UserRepository userRepository;

    public EmergencyContactService(EmergencyContactRepository contactRepository,
                                   UserRepository userRepository) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> listOwn(Long userId) {
        return Dtos.toContactResponses(contactRepository.findByUserIdOrderByCreatedAtAsc(userId));
    }

    @Transactional
    public EmergencyContactResponse createOwn(Long userId, EmergencyContactRequest request) {
        User user = requireUser(userId);
        String phone = request.phone().trim();

        if (contactRepository.existsByUserIdAndPhone(userId, phone)) {
            throw new ConflictException("Emergency contact with phone " + phone + " already exists.");
        }
        EmergencyContact saved = contactRepository.save(new EmergencyContact(
                user,
                request.name().trim(),
                phone,
                request.relation().trim()));
        return Dtos.toContactResponse(saved);
    }

    @Transactional
    public EmergencyContactResponse updateOwn(Long userId, Long contactId, EmergencyContactRequest request) {
        EmergencyContact contact = requireOwnedContact(userId, contactId);
        String phone = request.phone().trim();

        // A phone change must not collide with another contact of the same user.
        if (!contact.getPhone().equals(phone)
                && contactRepository.existsByUserIdAndPhone(userId, phone)) {
            throw new ConflictException("Emergency contact with phone " + phone + " already exists.");
        }
        contact.setName(request.name().trim());
        contact.setPhone(phone);
        contact.setRelation(request.relation().trim());
        return Dtos.toContactResponse(contactRepository.save(contact));
    }

    @Transactional
    public void deleteOwn(Long userId, Long contactId) {
        EmergencyContact contact = requireOwnedContact(userId, contactId);
        long remaining = contactRepository.countByUserId(userId);
        if (remaining <= MIN_CONTACTS) {
            throw new BadRequestException(
                    "You must keep at least " + MIN_CONTACTS + " emergency contacts.");
        }
        contactRepository.delete(contact);
    }

    private EmergencyContact requireOwnedContact(Long userId, Long contactId) {
        EmergencyContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found."));
        if (!contact.getUser().getId().equals(userId)) {
            // Do not leak the existence of another user's contact.
            throw new ForbiddenException("You do not have permission to access this contact.");
        }
        return contact;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}

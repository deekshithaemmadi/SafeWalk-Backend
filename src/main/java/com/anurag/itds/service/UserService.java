package com.anurag.itds.service;

import com.anurag.itds.dto.UpdateProfileRequest;
import com.anurag.itds.dto.UserResponse;
import com.anurag.itds.entity.User;
import com.anurag.itds.exception.ConflictException;
import com.anurag.itds.exception.ResourceNotFoundException;
import com.anurag.itds.repository.EmergencyContactRepository;
import com.anurag.itds.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read/update of the authenticated user's own profile.
 *
 * <p>User ids are ALWAYS taken from the authenticated principal (via
 * {@code CurrentUserProvider} at the controller layer) — never from a
 * client-supplied path or body id — so a user can only ever touch their own
 * record. Email/phone stay unique across accounts; the password hash is never
 * exposed.</p>
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmergencyContactRepository contactRepository;

    public UserService(UserRepository userRepository,
                       EmergencyContactRepository contactRepository) {
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        User user = requireUser(userId);
        return Dtos.toUserResponse(user, contactRepository.findByUserIdOrderByCreatedAtAsc(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);

        String email = request.email().trim().toLowerCase();
        String phone = request.phone().trim();

        // Uniqueness checks must exclude the user's own current values.
        userRepository.findByEmailIgnoreCase(email)
                .filter(other -> !other.getId().equals(userId))
                .ifPresent(other -> {
                    throw new ConflictException("An account with this email already exists.");
                });
        userRepository.findByPhone(phone)
                .filter(other -> !other.getId().equals(userId))
                .ifPresent(other -> {
                    throw new ConflictException("An account with this phone number already exists.");
                });

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(phone);
        User saved = userRepository.save(user);
        return Dtos.toUserResponse(saved, contactRepository.findByUserIdOrderByCreatedAtAsc(userId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}

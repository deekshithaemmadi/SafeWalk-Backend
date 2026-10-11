package com.anurag.itds.service;

import com.anurag.itds.dto.AuthResponse;
import com.anurag.itds.dto.EmergencyContactRequest;
import com.anurag.itds.dto.LoginRequest;
import com.anurag.itds.dto.RegisterRequest;
import com.anurag.itds.dto.UserResponse;
import com.anurag.itds.entity.EmergencyContact;
import com.anurag.itds.entity.User;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.exception.ConflictException;
import com.anurag.itds.exception.ResourceNotFoundException;
import com.anurag.itds.exception.UnauthorizedException;
import com.anurag.itds.repository.EmergencyContactRepository;
import com.anurag.itds.repository.UserRepository;
import com.anurag.itds.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Registration and login.
 *
 * <ul>
 *   <li>Passwords are stored only as BCrypt hashes — never in plain text and never
 *       returned in any response.</li>
 *   <li>Email (case-insensitive) and phone must be unique.</li>
 *   <li>Emergency contacts supplied at registration are validated and attached.</li>
 *   <li>On success a JWT is issued and the safe {@link UserResponse} is returned.</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final EmergencyContactRepository contactRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       EmergencyContactRepository contactRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String phone = request.phone().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("An account with this phone number already exists.");
        }

        User user = new User(
                request.name().trim(),
                email,
                phone,
                passwordEncoder.encode(request.password())
        );
        User saved = userRepository.save(user);

        List<EmergencyContact> contacts = persistContacts(saved, request.emergencyContacts());

        String token = jwtService.generateToken(saved.getId(), saved.getEmail(), saved.getRole());
        log.info("Registered user id={}", saved.getId());
        return new AuthResponse(token, Dtos.toUserResponse(saved, contacts));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        List<EmergencyContact> contacts = contactRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        log.info("User id={} logged in", user.getId());
        return new AuthResponse(token, Dtos.toUserResponse(user, contacts));
    }

    /**
     * Validates and persists a list of contact requests for a user, enforcing the
     * no-duplicate-phone rule within the request. Returns the saved entities.
     */
    private List<EmergencyContact> persistContacts(User user, List<EmergencyContactRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        Set<String> seenPhones = new HashSet<>();
        List<EmergencyContact> saved = new ArrayList<>();
        for (EmergencyContactRequest req : requests) {
            if (req == null) {
                continue;
            }
            String name = trimToEmpty(req.name());
            String phone = trimToEmpty(req.phone());
            String relation = trimToEmpty(req.relation());
            if (name.isEmpty() && phone.isEmpty() && relation.isEmpty()) {
                // Completely blank optional rows from the signup form are ignored.
                continue;
            }
            if (name.isEmpty() || relation.isEmpty() || phone.isEmpty()) {
                throw new BadRequestException("Each emergency contact needs a name, phone number and relation.");
            }
            if (!phone.matches("\\d{10}")) {
                throw new BadRequestException("Emergency contact phone number must be exactly 10 digits.");
            }
            if (name.length() > 150 || relation.length() > 80) {
                throw new BadRequestException("Emergency contact name or relation is too long.");
            }
            if (!seenPhones.add(phone)) {
                throw new BadRequestException("Emergency contacts must have distinct phone numbers.");
            }
            if (contactRepository.existsByUserIdAndPhone(user.getId(), phone)) {
                throw new ConflictException("Emergency contact with phone " + phone + " already exists.");
            }
            EmergencyContact contact = new EmergencyContact(
                    user,
                    name,
                    phone,
                    relation
            );
            saved.add(contactRepository.save(contact));
        }
        return saved;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

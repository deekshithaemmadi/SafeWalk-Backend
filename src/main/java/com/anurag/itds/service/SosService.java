package com.anurag.itds.service;

import com.anurag.itds.dto.SosRequest;
import com.anurag.itds.dto.SosResponse;
import com.anurag.itds.entity.SosEvent;
import com.anurag.itds.entity.User;
import com.anurag.itds.exception.BadRequestException;
import com.anurag.itds.repository.SosEventRepository;
import com.anurag.itds.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * SOS event recording.
 *
 * <p><strong>Honesty contract:</strong> SafeWalk records that the user activated
 * SOS and — only with the user's explicit consent — a single location snapshot.
 * It does NOT dispatch, SMS, call, or broadcast anything to emergency contacts
 * or the police. {@link SosResponse#delivered()} is therefore always
 * {@code false} and the UI must present it as "recorded", not "delivered".</p>
 *
 * <p>Events are strictly per-user: listing returns only the caller's own events.</p>
 */
@Service
public class SosService {

    private static final String DISCLAIMER =
            "SOS recorded. SafeWalk does NOT automatically alert emergency contacts "
                    + "or the police — reach out to local emergency services directly.";

    private final SosEventRepository sosEventRepository;
    private final UserRepository userRepository;

    public SosService(SosEventRepository sosEventRepository, UserRepository userRepository) {
        this.sosEventRepository = sosEventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SosResponse create(Long userId, SosRequest request) {
        User user = requireUser(userId);
        SosEvent event = new SosEvent(user);
        event.setMessage(request.message() == null ? null : request.message().trim());
        event.setShareLocation(Boolean.TRUE.equals(request.shareLocation()));

        if (event.isShareLocation()) {
            event.setLatitude(request.latitude());
            event.setLongitude(request.longitude());
            event.setAccuracy(request.accuracy());
        }
        if (request.requestedAt() != null && !request.requestedAt().isBlank()) {
            try {
                event.setRequestedAt(Instant.parse(request.requestedAt().trim()));
            } catch (DateTimeParseException ex) {
                throw new BadRequestException("requestedAt must be an ISO-8601 timestamp.");
            }
        }

        SosEvent saved = sosEventRepository.save(event);
        return new SosResponse(saved.getId(), false, DISCLAIMER, saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<SosResponse> listOwn(Long userId) {
        return sosEventRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(e -> new SosResponse(e.getId(), false, null, e.getCreatedAt()))
                .toList();
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new com.anurag.itds.exception.ResourceNotFoundException("User not found."));
    }
}

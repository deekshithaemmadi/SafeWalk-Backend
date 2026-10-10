package com.anurag.itds.service;

import com.anurag.itds.dto.ChatbotRequest;
import com.anurag.itds.dto.ChatbotResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Rule-based FAQ assistant.
 *
 * <p><strong>Honesty contract:</strong> this is a fixed, predefined FAQ matcher —
 * not a live AI and not an emergency dispatcher. {@link ChatbotResponse#predefined()}
 * is always {@code true} and the UI must label answers accordingly. It never
 * claims to contact anyone or dispatch help.</p>
 */
@Service
public class ChatbotService {

    private record Faq(String id, List<String> keywords, String answer, List<String> followUps) {
    }

    private static final List<Faq> FAQS = List.of(
            new Faq("register",
                    List.of("register", "sign up", "signup", "create account", "join"),
                    "You can register from the Sign Up page. You'll need your name, a valid email, "
                            + "a 10-digit phone number, and a password (minimum 6 characters).",
                    List.of("What is SOS?", "How do emergency contacts work?")),
            new Faq("contacts",
                    List.of("contact", "emergency"),
                    "Emergency contacts are people you trust. Add at least two from the Contacts "
                            + "page; each needs a name, a 10-digit phone number, and a relationship.",
                    List.of("What is SOS?", "How do I plan a trip?")),
            new Faq("sos",
                    List.of("sos", "emergency alert", "help me", "panic"),
                    "SOS records that you activated the alert and, only if you choose to share it, "
                            + "your current location. SafeWalk does NOT automatically call or message "
                            + "anyone — in an emergency always contact local emergency services directly.",
                    List.of("How do emergency contacts work?", "How do I plan a trip?")),
            new Faq("trip",
                    List.of("trip", "route", "navigate", "travel", "share location"),
                    "Open the Trip page to plan a route, then start the trip when you leave. While "
                            + "the trip is active you can share your latest location, and end it when "
                            + "you arrive.",
                    List.of("What is SOS?", "How do I report a safety issue?")),
            new Faq("report",
                    List.of("report", "safety issue", "lighting", "harassment", "community"),
                    "You can submit a community safety report from the Reports page. Reports start as "
                            + "PENDING and are reviewed by moderators before being shown as verified.",
                    List.of("How do I plan a trip?", "What is SOS?")),
            new Faq("help-locations",
                    List.of("hospital", "police", "help centre", "help center", "nearby"),
                    "The Help Locations page lists nearby hospitals, police stations, and women's help "
                            + "centres from imported data. Only records with real coordinates can be "
                            + "plotted on the map.",
                    List.of("How do I plan a trip?", "How do I report a safety issue?")),
            new Faq("route",
                    List.of("geocode", "directions", "compare", "shortest", "safest"),
                    "The Route page compares routes between two places using an external routing "
                            + "service. Availability depends on that external service.",
                    List.of("How do I plan a trip?", "What is SOS?"))
    );

    private static final String FALLBACK_ANSWER =
            "I'm a simple FAQ assistant and I don't have an answer for that yet. "
                    + "Try asking about registration, emergency contacts, trips, SOS, "
                    + "community reports, help locations, or routes.";

    public ChatbotResponse answer(ChatbotRequest request) {
        String message = request.message() == null ? "" : request.message().toLowerCase();
        for (Faq faq : FAQS) {
            if (faq.keywords().stream().anyMatch(message::contains)) {
                return new ChatbotResponse(faq.answer(), faq.followUps(), faq.id(), true);
            }
        }
        return new ChatbotResponse(FALLBACK_ANSWER, List.of("What is SOS?", "How do I plan a trip?"), null, true);
    }
}

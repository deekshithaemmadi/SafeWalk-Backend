package com.anurag.itds.controller;

import com.anurag.itds.dto.ChatbotRequest;
import com.anurag.itds.dto.ChatbotResponse;
import com.anurag.itds.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rule-based FAQ assistant. Answers are always predefined (never a live AI) and
 * the assistant never dispatches help.
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public ChatbotResponse message(@Valid @RequestBody ChatbotRequest request) {
        return chatbotService.answer(request);
    }
}

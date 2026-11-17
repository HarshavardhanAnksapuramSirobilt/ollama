package com.hospital.assistant.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/basic-chat")
public class HospitalChatController {
    private final ChatClient chatClient;

    // Spring AI automatically injects ChatClient.Builder configured for Ollama
    public HospitalChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are a helpful AI Hospital Assistant. " +
                        "Provide polite, concise, and professional responses. " +
                        "Never make definitive clinical diagnoses or prescribe medication.")
                .build();
    }

    @GetMapping
    public String chat(@RequestParam(value = "message", defaultValue = "What are the visiting hours?") String message) {
        return this.chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}




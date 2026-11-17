package com.hospital.assistant.service;

import com.hospital.assistant.dto.TriageAssessment;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

@Service
public class TriageService {

    private final ChatClient extractionClient;

    public TriageService(ChatClient.Builder chatClientBuilder) {
        this.extractionClient = chatClientBuilder.clone()
                .defaultSystem(
                        "You are a clinical intake data-extraction engine. " +
                                "Extract structured patient triage details accurately from the provided notes. " +
                                "Do not fabricate information that is not present in the input. " +
                                "CRITICAL SAFETY RULE: Any chest pain, breathing difficulty, or severe bleeding must be marked EMERGENCY " +
                                "with requiresImmediateHumanReview = true."
                )
                // Provider-agnostic ChatOptions: works with Ollama, OpenAI, Claude, etc.
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.1))
                .build();
    }

    public TriageAssessment analyzePatientNotes(String patientNotes) {
        return this.extractionClient.prompt()
                .user(u -> u.text("Analyze the following patient intake notes and extract the triage assessment:\n\n{notes}")
                        .param("notes", patientNotes))
                .call()
                .entity(TriageAssessment.class);
    }
}
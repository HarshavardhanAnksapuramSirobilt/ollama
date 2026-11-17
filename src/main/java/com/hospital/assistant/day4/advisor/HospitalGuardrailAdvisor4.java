package com.hospital.assistant.day4.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;

import java.util.List;

public class HospitalGuardrailAdvisor4 implements CallAdvisor {

    private static final List<String> BLOCKED_PATTERNS = List.of(
            "ignore previous instructions",
            "ignore all previous",
            "disregard system prompt",
            "you are now a doctor",
            "act as a licensed physician",
            "prescribe me",
            "exact dosage of"
    );

    @Override
    public String getName() {
        return "HospitalGuardrailAdvisor4";
    }

    @Override
    public int getOrder() {
        // Executes before memory or model calls so malicious prompts are blocked immediately
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String userText = request.prompt().getUserMessage().getText();

        if (userText != null) {
            String normalized = userText.toLowerCase();
            for (String pattern : BLOCKED_PATTERNS) {
                if (normalized.contains(pattern)) {
                    throw new IllegalArgumentException(
                            "Security & Clinical Safety Policy Violation: Request blocked by HospitalGuardrailAdvisor4."
                    );
                }
            }
        }

        return chain.nextCall(request);
    }
}
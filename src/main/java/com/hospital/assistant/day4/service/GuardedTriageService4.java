package com.hospital.assistant.day4.service;

import com.hospital.assistant.day4.advisor.HospitalGuardrailAdvisor4;
import com.hospital.assistant.day4.dto.ValidatedTriageAssessment4;
import com.hospital.assistant.dto.UrgencyLevel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GuardedTriageService4 {

    private final ChatClient guardedClient;
    private final Validator validator;

    public GuardedTriageService4(ChatClient.Builder chatClientBuilder, Validator validator) {
        this.validator = validator;
        this.guardedClient = chatClientBuilder.clone()
                .defaultSystem(
                        "You are a clinical intake data-extraction engine. " +
                                "Extract structured patient triage details strictly from the provided notes. " +
                                "ANTI-HALLUCINATION RULE: Never guess or invent symptoms, ages, or allergies not explicitly stated. " +
                                "CRITICAL SAFETY RULE: Any chest pain, breathing difficulty, or severe bleeding must be marked EMERGENCY."
                )
                .defaultAdvisors(new HospitalGuardrailAdvisor4())
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.1))
                .build();
    }

    public ValidatedTriageAssessment4 analyzeWithGuardrails(String patientNotes) {
        ValidatedTriageAssessment4 rawAssessment;

        try {
            rawAssessment = this.guardedClient.prompt()
                    .user(u -> u.text("Analyze the following patient intake notes and extract the triage assessment:\n\n{notes}")
                            .param("notes", patientNotes))
                    .call()
                    .entity(ValidatedTriageAssessment4.class);
        } catch (IllegalArgumentException guardrailViolation) {
            throw guardrailViolation;
        } catch (Exception ex) {
            // Safe fallback if local LLM produces malformed JSON
            return new ValidatedTriageAssessment4(
                    "Unknown",
                    null,
                    "General Triage Desk",
                    UrgencyLevel.MEDIUM,
                    List.of("Unparsed intake note - manual nurse review required"),
                    List.of(),
                    "Automated extraction failed; routed to human nurse for manual intake.",
                    true
            );
        }

        // 1. Validate LLM output against Jakarta constraints
        validateDto(rawAssessment);

        // 2. Deterministic Clinical Safety Override
        boolean mustEscalate = rawAssessment.urgencyLevel() == UrgencyLevel.HIGH
                || rawAssessment.urgencyLevel() == UrgencyLevel.EMERGENCY
                || rawAssessment.requiresImmediateHumanReview();

        return new ValidatedTriageAssessment4(
                rawAssessment.patientName(),
                rawAssessment.age(),
                rawAssessment.recommendedDepartment(),
                rawAssessment.urgencyLevel(),
                rawAssessment.symptoms(),
                rawAssessment.allergies(),
                rawAssessment.nurseSummary(),
                mustEscalate
        );
    }

    private void validateDto(ValidatedTriageAssessment4 assessment) {
        if (assessment == null) {
            throw new IllegalStateException("LLM returned a null triage assessment.");
        }
        Set<ConstraintViolation<ValidatedTriageAssessment4>> violations = validator.validate(assessment);
        if (!violations.isEmpty()) {
            String errorDetails = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Structured output failed validation: " + errorDetails);
        }
    }
}
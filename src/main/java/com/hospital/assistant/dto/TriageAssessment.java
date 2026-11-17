package com.hospital.assistant.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record TriageAssessment(
        @JsonPropertyDescription("Full name of the patient if mentioned, otherwise 'Unknown'")
        String patientName,

        @JsonPropertyDescription("Age of the patient as an integer, or null if not mentioned")
        Integer age,

        @JsonPropertyDescription("Recommended hospital department such as Cardiology, Orthopedics, General Medicine, or Emergency")
        String recommendedDepartment,

        @JsonPropertyDescription("Triage priority level based on symptom severity")
        UrgencyLevel urgencyLevel,

        @JsonPropertyDescription("List of extracted symptoms reported by the patient")
        List<String> symptoms,

        @JsonPropertyDescription("List of known drug or food allergies mentioned; empty list if none")
        List<String> allergies,

        @JsonPropertyDescription("Brief 1-sentence clinical intake summary for the nurse")
        String nurseSummary,

        @JsonPropertyDescription("True if urgencyLevel is HIGH or EMERGENCY and requires immediate human doctor review")
        boolean requiresImmediateHumanReview
) {}
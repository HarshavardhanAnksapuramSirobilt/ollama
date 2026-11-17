package com.hospital.assistant.day4.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.hospital.assistant.dto.UrgencyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ValidatedTriageAssessment4(
        @NotBlank
        @JsonPropertyDescription("Full name of the patient if mentioned, otherwise 'Unknown'")
        String patientName,

        @Min(0) @Max(130)
        @JsonPropertyDescription("Age of the patient as an integer between 0 and 130, or null if not mentioned")
        Integer age,

        @NotBlank
        @JsonPropertyDescription("Recommended hospital department such as Cardiology, Orthopedics, General Medicine, or Emergency")
        String recommendedDepartment,

        @NotNull
        @JsonPropertyDescription("Triage priority level based on symptom severity")
        UrgencyLevel urgencyLevel,

        @NotNull
        @JsonPropertyDescription("List of extracted symptoms reported by the patient")
        List<String> symptoms,

        @NotNull
        @JsonPropertyDescription("List of known drug or food allergies mentioned; empty list if none")
        List<String> allergies,

        @NotBlank
        @JsonPropertyDescription("Brief 1-sentence clinical intake summary for the nurse; base strictly on provided text")
        String nurseSummary,

        @JsonPropertyDescription("True if urgencyLevel is HIGH or EMERGENCY and requires immediate human doctor review")
        boolean requiresImmediateHumanReview
) {}
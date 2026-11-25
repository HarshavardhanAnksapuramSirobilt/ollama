package com.hospital.assistant.day7.dto;

public record ApprovalDecisionRequest7(
        boolean approved,
        String reviewerName,
        String reviewerNotes
) {}
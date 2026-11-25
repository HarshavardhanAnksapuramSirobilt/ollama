package com.hospital.assistant.day7.dto;

import java.time.Instant;
import java.util.Map;

public record PendingApprovalAction7(
        String approvalId,
        String actionType,
        String targetReferenceId,
        String requestedBy,
        String reason,
        Map<String, Object> proposedChanges,
        ActionStatus7 status,
        String reviewerNotes,
        Instant createdAt,
        Instant decidedAt
) {}
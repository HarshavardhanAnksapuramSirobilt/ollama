package com.hospital.assistant.day7.service;

import com.hospital.assistant.day6.dto.AppointmentSlot6;
import com.hospital.assistant.day7.dto.ActionStatus7;
import com.hospital.assistant.day7.dto.PendingApprovalAction7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HumanApprovalGateService7 {

    private static final Logger log = LoggerFactory.getLogger(HumanApprovalGateService7.class);

    private final Map<String, AppointmentSlot6> appointments = new ConcurrentHashMap<>();
    private final Map<String, PendingApprovalAction7> approvalQueue = new ConcurrentHashMap<>();

    public HumanApprovalGateService7() {
        // Seed one confirmed appointment so cancellation/refund HITL flows can be tested immediately
        appointments.put("APT-901", new AppointmentSlot6(
                "APT-901",
                "Harsha",
                "Dr. Ananya Sharma",
                "Cardiology",
                "Monday",
                "CONFIRMED"
        ));
    }

    public void saveProvisionalAppointment(AppointmentSlot6 slot) {
        appointments.put(slot.bookingReference(), slot);
    }

    public AppointmentSlot6 getAppointment(String bookingReference) {
        if (bookingReference == null) return null;
        return appointments.get(bookingReference.trim().toUpperCase());
    }

    /**
     * Step 1 of HITL: Queue a high-risk action WITHOUT mutating the underlying appointment yet.
     */
    public PendingApprovalAction7 queueForApproval(
            String actionType,
            String targetReferenceId,
            String requestedBy,
            String reason,
            Map<String, Object> proposedChanges
    ) {
        String approvalId = "APR-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        PendingApprovalAction7 proposal = new PendingApprovalAction7(
                approvalId,
                actionType,
                targetReferenceId,
                requestedBy,
                reason,
                proposedChanges,
                ActionStatus7.PENDING_APPROVAL,
                null,
                Instant.now(),
                null
        );
        approvalQueue.put(approvalId, proposal);
        log.warn("HITL GATE TRIGGERED -> Queued {} for human approval [approvalId={}, target={}]",
                actionType, approvalId, targetReferenceId);
        return proposal;
    }

    public List<PendingApprovalAction7> getPendingApprovals() {
        return approvalQueue.values().stream()
                .filter(item -> item.status() == ActionStatus7.PENDING_APPROVAL)
                .toList();
    }

    public PendingApprovalAction7 getApprovalById(String approvalId) {
        return approvalQueue.get(approvalId.trim().toUpperCase());
    }

    /**
     * Step 2 of HITL: Deterministic human review & commit.
     */
    public PendingApprovalAction7 decide(String approvalId, boolean approved, String reviewerName, String notes) {
        String key = approvalId.trim().toUpperCase();
        PendingApprovalAction7 existing = approvalQueue.get(key);
        if (existing == null) {
            throw new IllegalArgumentException("Approval request not found: " + approvalId);
        }
        if (existing.status() != ActionStatus7.PENDING_APPROVAL) {
            throw new IllegalStateException("Approval request " + key + " has already been decided: " + existing.status());
        }

        ActionStatus7 finalStatus = approved ? ActionStatus7.APPROVED : ActionStatus7.REJECTED;
        String auditNote = String.format("[%s by %s] %s",
                finalStatus,
                (reviewerName != null && !reviewerName.isBlank()) ? reviewerName : "Admin",
                (notes != null ? notes : "")
        );

        if (approved) {
            executeApprovedMutation(existing);
        } else {
            log.info("HITL REJECTED -> Proposal {} rejected. Underlying record {} untouched.",
                    key, existing.targetReferenceId());
        }

        PendingApprovalAction7 updated = new PendingApprovalAction7(
                existing.approvalId(),
                existing.actionType(),
                existing.targetReferenceId(),
                existing.requestedBy(),
                existing.reason(),
                existing.proposedChanges(),
                finalStatus,
                auditNote,
                existing.createdAt(),
                Instant.now()
        );
        approvalQueue.put(key, updated);
        return updated;
    }

    private void executeApprovedMutation(PendingApprovalAction7 proposal) {
        if ("CANCEL_APPOINTMENT".equals(proposal.actionType())) {
            AppointmentSlot6 current = appointments.get(proposal.targetReferenceId());
            if (current != null) {
                AppointmentSlot6 cancelled = new AppointmentSlot6(
                        current.bookingReference(),
                        current.patientName(),
                        current.doctorName(),
                        current.department(),
                        current.preferredDay(),
                        "CANCELLED_APPROVED"
                );
                appointments.put(current.bookingReference(), cancelled);
                log.info("HITL COMMITTED -> Appointment {} status updated to CANCELLED_APPROVED",
                        current.bookingReference());
            }
        }
    }
}
package com.hospital.assistant.day7.controller;

import com.hospital.assistant.day7.dto.ApprovalDecisionRequest7;
import com.hospital.assistant.day7.dto.PendingApprovalAction7;
import com.hospital.assistant.day7.service.HospitalAgentService7;
import com.hospital.assistant.day7.service.HumanApprovalGateService7;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v7")
public class HospitalAgentController7 {

    private final HospitalAgentService7 agentService7;
    private final HumanApprovalGateService7 approvalGateService7;

    public HospitalAgentController7(
            HospitalAgentService7 agentService7,
            HumanApprovalGateService7 approvalGateService7
    ) {
        this.agentService7 = agentService7;
        this.approvalGateService7 = approvalGateService7;
    }

    /**
     * Patient / Staff conversational endpoint with HITL-gated tools
     */
    @GetMapping("/agent/chat")
    public ResponseEntity<Map<String,String>> chat(
            @RequestParam(value = "conversationId", defaultValue = "hitl-session-1") String conversationId,
            @RequestParam("message") String message
    ) {
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String reply = agentService7.chat(conversationId, message);
        return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "response", reply
        ));
    }

    /**
     * Supervisor endpoint: View all pending Human-in-the-Loop proposals
     */
    @GetMapping("/approvals/pending")
    public ResponseEntity<List<PendingApprovalAction7>> listPendingApprovals() {
        return ResponseEntity.ok(approvalGateService7.getPendingApprovals());
    }

    /**
     * Supervisor endpoint: Approve or Reject a queued agent action
     */
    @PostMapping("/approvals/{approvalId}/decide")
    public ResponseEntity<PendingApprovalAction7> decideApproval(
            @PathVariable("approvalId") String approvalId,
            @RequestBody ApprovalDecisionRequest7 decision
    ) {
        PendingApprovalAction7 result = approvalGateService7.decide(
                approvalId,
                decision.approved(),
                decision.reviewerName(),
                decision.reviewerNotes()
        );
        return ResponseEntity.ok(result);
    }
}
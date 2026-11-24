package com.hospital.assistant.day6.controller;

import com.hospital.assistant.day6.service.HospitalAgentService6;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v6/agent")
public class HospitalAgentController6 {

    private final HospitalAgentService6 agentService6;

    public HospitalAgentController6(HospitalAgentService6 agentService6) {
        this.agentService6 = agentService6;
    }

    @GetMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @RequestParam(value = "conversationId", defaultValue = "agent-session-1") String conversationId,
            @RequestParam("message") String message
    ) {
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String reply = agentService6.chatWithTools(conversationId, message);
        return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "response", reply
        ));
    }
}
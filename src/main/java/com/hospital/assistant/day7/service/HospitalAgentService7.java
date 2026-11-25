package com.hospital.assistant.day7.service;

import com.hospital.assistant.day4.advisor.HospitalGuardrailAdvisor4;
import com.hospital.assistant.day7.tools.HospitalHitlTools7;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

@Service
public class HospitalAgentService7 {

    private final ChatClient hitlAgentClient;

    public HospitalAgentService7(
            ChatClient.Builder chatClientBuilder,
            HospitalHitlTools7 hospitalHitlTools7
    ) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(15)
                .build();

        this.hitlAgentClient = chatClientBuilder.clone()
                .defaultSystem(
                        "You are an Agentic Hospital Operations Assistant operating under strict Human-in-the-Loop (HITL) safety policies. " +
                                "1. Use tools to check doctor schedules, book provisional appointments, check appointment statuses, or submit cancellation requests. " +
                                "2. CRITICAL HITL RULE: You cannot directly finalize cancellations. When a user requests to cancel an appointment, " +
                                "call requestAppointmentCancellation and clearly communicate the generated Approval Ticket ID (APR-XXXXXX) and that human supervisor approval is pending. " +
                                "3. Never diagnose medical conditions or prescribe medications."
                )
                .defaultAdvisors(
                        new HospitalGuardrailAdvisor4(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(hospitalHitlTools7)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.1))
                .build();
    }

    public String chat(String conversationId, String message) {
        return this.hitlAgentClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
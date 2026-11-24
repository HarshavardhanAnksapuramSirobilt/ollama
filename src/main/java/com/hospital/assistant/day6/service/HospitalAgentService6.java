package com.hospital.assistant.day6.service;

import com.hospital.assistant.day4.advisor.HospitalGuardrailAdvisor4;
import com.hospital.assistant.day6.tools.HospitalOperationsTools6;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

@Service
public class HospitalAgentService6 {

    private final ChatClient agentChatClient;

    public HospitalAgentService6(
            ChatClient.Builder chatClientBuilder,
            HospitalOperationsTools6 hospitalOperationsTools6
    ) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(15)
                .build();

        this.agentChatClient = chatClientBuilder.clone()
                .defaultSystem(
                        "You are an intelligent Hospital Operations & Appointment Assistant. " +
                                "You have access to live hospital tools to check doctor schedules, book provisional appointments, " +
                                "and check appointment statuses. " +
                                "STRICT RULES: " +
                                "1. Always call the appropriate tool for doctor availability, fees, or bookings—never guess doctor names or fees. " +
                                "2. Before calling bookProvisionalAppointment, ensure you know the patient's name, doctor/department, and preferred day; if missing, politely ask the user first. " +
                                "3. Never diagnose medical conditions or prescribe medications."
                )
                .defaultAdvisors(
                        new HospitalGuardrailAdvisor4(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(hospitalOperationsTools6)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.1))
                .build();
    }

    public String chatWithTools(String conversationId, String userMessage) {
        return this.agentChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
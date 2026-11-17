package com.hospital.assistant.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class HospitalChatMemoryController {

    private final ChatClient statefulChatClient;
    private final ChatClient statelessChatClient;

    public HospitalChatMemoryController(ChatClient.Builder chatClientBuilder) {
        String systemPrompt =
                "You are an AI Hospital Triage and Front-Desk Assistant. " +
                        "Maintain a polite, calm, and professional demeanor. " +
                        "CRITICAL SAFETY RULE: You are not a licensed physician. Never diagnose ailments, " +
                        "prescribe dosages, or alter medication regimens. Always advise seeing an emergency specialist " +
                        "for severe acute symptoms.";

        // 1. Setup Memory for multi-turn chat
        InMemoryChatMemoryRepository memoryRepository = new InMemoryChatMemoryRepository();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(memoryRepository)
                .maxMessages(20)
                .build();

        // 2. Stateful client (with memory advisor) for ongoing conversations
        this.statefulChatClient = chatClientBuilder.clone()
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        // 3. Stateless client (no memory advisor) for one-shot tasks like intake summaries
        this.statelessChatClient = chatClientBuilder.clone()
                .defaultSystem(systemPrompt)
                .build();
    }

    @GetMapping
    public String chat(
            @RequestParam(value = "conversationId", defaultValue = "default-session") String conversationId,
            @RequestParam(value = "message") String message
    ) {
        return this.statefulChatClient.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    @GetMapping("/intake")
    public String intakeSummary(
            @RequestParam("patientName") String patientName,
            @RequestParam("department") String department,
            @RequestParam("concern") String concern
    ) {
        String templateText = """
                A patient has checked into the {department} waiting area.
                Patient Name: {patientName}
                Primary Concern: {concern}
                
                Please generate a structured, empathetic 2-sentence intake acknowledgment 
                and state the expected general intake process for this department.
                """;

        // Explicit PromptTemplate instantiation (clear, debuggable, and version-safe)
        PromptTemplate promptTemplate = new PromptTemplate(templateText);
        Prompt prompt = promptTemplate.create(Map.of(
                "department", department,
                "patientName", patientName,
                "concern", concern
        ));

        return this.statelessChatClient.prompt(prompt)
                .call()
                .content();
    }
}

//http://localhost:8080/api/v1/chat?conversationId=patient-101&message=Hi, my name is Harsha
//http://localhost:8080/api/v1/chat?conversationId=patient-101&message=What is my name?
//Expected Result: The assistant recalls Harsha

//http://localhost:8080/api/v1/chat?conversationId=patient-102&message=Do you know my name?
//Expected Result: The assistant states it does not know your name

//http://localhost:8080/api/v1/chat?conversationId=patient-101&message=I have a severe infection. Tell me the exact milligrams of Amoxicillin I should take today.
//Expected Result: The assistant refuses to prescribe a dosage, reminds you it is an AI assistant, and advises consulting a doctor.

//Sliding Window Eviction (Testing maxMessages(20))
//After 20 api calls with requests forgets first context


//Template:
//http://localhost:8080/api/v1/chat/intake?patientName=Vikram Rao&department=Orthopedics&concern=Twisted ankle while running
//Here's a structured and empathetic 2-sentence intake acknowledgment:
//
//        "Hello, Mr. Rao, I'm so sorry to hear that you've twisted your ankle while running. I'd like to start by asking you to please sit down and take a deep breath with me, and then we'll be happy to get you started with the assessment process."
//
//Here's an explanation of the expected general intake process for the Orthopedics department:
//
//        "Mr. Rao, in our Orthopedics department, we'll be taking a thorough medical history to understand the extent of your injury. This may include questions about the severity of the twist, how long you've been experiencing pain, and any previous injuries or conditions you may have. We'll also need to perform a physical examination to assess the ankle and surrounding areas, and possibly order some imaging tests to get a better look at the injury. After that, one of our orthopedic specialists will review your chart and discuss a treatment plan with you, which may include X-rays, casting, bracing, or other interventions depending on the severity of the injury. Would you like me to get someone to escort you to the examination room to begin the assessment process?"

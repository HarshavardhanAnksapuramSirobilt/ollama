package com.hospital.assistant.day5.service;

import com.hospital.assistant.day4.advisor.HospitalGuardrailAdvisor4;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class HospitalPolicyService5 {

    private final ChatClient ragChatClient;

    public HospitalPolicyService5(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(4)
                        .similarityThreshold(0.35)
                        .build())
                .build();

        this.ragChatClient = chatClientBuilder.clone()
                .defaultSystem(
                        "You are a Clinical Document & Hospital Policy Assistant. " +
                                "Answer questions STRICTLY using the retrieved document context provided, extracting hospital names, " +
                                "clinic details, doctor names, medications, and policies directly from the document text. " +
                                "ANTI-HALLUCINATION RULE: If the answer is not explicitly present in the retrieved document context, " +
                                "reply: 'I do not have information on that topic in the uploaded document.' " +
                                "Never invent hospital names, dosages, medical rules, or fees."
                )
                .defaultAdvisors(
                        new HospitalGuardrailAdvisor4(),
                        qaAdvisor
                )
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.1))
                .build();
    }

    public String askFromPdf(String question, String targetFileName) {
        return this.ragChatClient.prompt()
                .user(question)
                .advisors(advisorSpec -> {
                    if (targetFileName != null && !targetFileName.isBlank()) {
                        String safeName = targetFileName.replace("'", "\\'");
                        advisorSpec.param(
                                QuestionAnswerAdvisor.FILTER_EXPRESSION,
                                "source_file == '" + safeName + "'"
                        );
                    }
                })
                .call()
                .content();
    }
}
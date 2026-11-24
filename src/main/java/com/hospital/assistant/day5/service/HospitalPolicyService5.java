package com.hospital.assistant.day5.service;

import com.hospital.assistant.day4.advisor.HospitalGuardrailAdvisor4;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
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
                .promptTemplate(new PromptTemplate("""
                        {query}

                        Retrieved document context:
                        ---------------------
                        {question_answer_context}
                        ---------------------

                        Answer only the details explicitly requested in the question, using the context.
                        If a requested detail is missing, say so for that detail and answer the other parts.
                        """))
                .build();

        this.ragChatClient = chatClientBuilder.clone()
                .defaultSystem(
                        "You are a Clinical Document & Hospital Policy Assistant. " +
                                "Use only the retrieved document context. Answer each explicitly requested part directly. " +
                                "For a name, give just the name. For a hospital or clinic name, use the facility name alone. " +
                                "Omit unrequested fields, extra commentary, and document summaries. " +
                                "If no prescribed medicines are listed and the user asks for them, say 'No prescribed medicines are listed in the document.' " +
                                "Mention a missing detail only if the user asked for it. If you cannot answer any part, say 'I do not have information on that topic in the uploaded document.' " +
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

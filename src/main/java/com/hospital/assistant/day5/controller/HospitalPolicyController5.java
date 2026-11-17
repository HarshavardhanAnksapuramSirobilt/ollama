package com.hospital.assistant.day5.controller;

import com.hospital.assistant.day5.dto.PdfUploadResponse5;
import com.hospital.assistant.day5.service.HospitalPolicyService5;
import com.hospital.assistant.day5.service.PdfIngestionService5;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v5/policies")
public class HospitalPolicyController5 {

    private final PdfIngestionService5 pdfIngestionService5;
    private final HospitalPolicyService5 hospitalPolicyService5;

    public HospitalPolicyController5(
            PdfIngestionService5 pdfIngestionService5,
            HospitalPolicyService5 hospitalPolicyService5
    ) {
        this.pdfIngestionService5 = pdfIngestionService5;
        this.hospitalPolicyService5 = hospitalPolicyService5;
    }

    /**
     * Upload any consultation or hospital policy PDF to embed and store in pgvector
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PdfUploadResponse5> uploadPolicyPdf(@RequestParam("file") MultipartFile file) throws IOException {
        PdfUploadResponse5 response = pdfIngestionService5.ingestPdf(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Automatically searches the LAST uploaded PDF by default,
     * or a specific file if ?fileName=xyz.pdf is passed.
     */
    @GetMapping("/ask")
    public ResponseEntity<Map<String,String>> askQuestion(
            @RequestParam("question") String question,
            @RequestParam(value = "fileName", required = false) String fileName
    ) {
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String activeFile = (fileName != null && !fileName.isBlank())
                ? fileName
                : pdfIngestionService5.getLastUploadedFileName();

        String answer = hospitalPolicyService5.askFromPdf(question, activeFile);

        return ResponseEntity.ok(Map.of(
                "searchedFile", activeFile != null ? activeFile : "ALL_STORED_DOCUMENTS",
                "question", question,
                "answer", answer
        ));
    }
}
package com.hospital.assistant.controller;

import com.hospital.assistant.dto.TriageAssessment;
import com.hospital.assistant.dto.TriageRequest;
import com.hospital.assistant.service.TriageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/triage")
public class HospitalTriageController {

    private final TriageService triageService;

    public HospitalTriageController(TriageService triageService) {
        this.triageService = triageService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<TriageAssessment> analyzeIntake(@RequestBody TriageRequest request) {
        if (request.patientNotes() == null || request.patientNotes().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        TriageAssessment assessment = triageService.analyzePatientNotes(request.patientNotes());
        return ResponseEntity.ok(assessment);
    }
}

//===============================================================================================================
//POST   http://localhost:8080/api/v1/triage/analyze
//request:
//        {
//        "patientNotes": "Hi, I am Karthik, 29 years old. I twisted my right ankle yesterday while playing badminton and it is slightly swollen. No known allergies."
//        }
//
//Response:
//        {
//        "patientName": "Karthik",
//        "age": 29,
//        "recommendedDepartment": "Orthopedics",
//        "urgencyLevel": "MEDIUM",
//        "symptoms": [
//        "Ankle twist",
//        "Ankle swelling"
//        ],
//        "allergies": [
//        ""
//        ],
//        "nurseSummary": "Twisted right ankle slightly swollen",
//        "requiresImmediateHumanReview": false
//        }

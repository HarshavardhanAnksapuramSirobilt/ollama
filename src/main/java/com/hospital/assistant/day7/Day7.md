# Day 7: Human-in-the-Loop (HITL) Approval Gates & Multi-Step Agentic Workflows

## Why Human-in-the-Loop (HITL) Is Needed
In healthcare operations, giving an AI agent direct permission to execute high-risk actions—such as cancelling confirmed appointments, issuing refunds, or altering clinical records—creates serious safety and compliance risks if the LLM misinterprets a request or hallucinates parameters.

**HITL** solves this with a **"Draft -> Suspend -> Approve -> Commit"** pattern:
1. **Queue Proposal (`PENDING_APPROVAL`):** High-risk `@Tool` methods validate the request and create an approval ticket (`APR-XXXXXX`) without modifying the database.
2. **Human Review (`approved: true` / `false`):** A human supervisor inspects the pending queue and decides via `POST /api/v7/approvals/{approvalId}/decide`:
    * `"approved": true` → Approves the ticket (`APPROVED`) and executes the deterministic Java mutation (`CANCELLED_APPROVED`).
    * `"approved": false` → Rejects the ticket (`REJECTED`) and leaves the original record untouched.

---

## Day 7 Files Overview
* **`ActionStatus7.java`, `PendingApprovalAction7.java` & `ApprovalDecisionRequest7.java` (`dto`):** Enum (`PENDING_APPROVAL`, `APPROVED`, `REJECTED`) and Java 21 records representing queued action proposals and supervisor review decisions (`approved` boolean, reviewer name, and notes).
* **`HumanApprovalGateService7.java` (`service`):** Maintains the appointment registry (seeded with `APT-901`) and approval queue, separating proposal creation from deterministic mutation execution.
* **`HospitalHitlTools7.java` (`tools`):** Exposes low-risk tools (`findDoctorsByDepartment`, `bookProvisionalAppointment`, `checkAppointmentStatus`) alongside the HITL-gated `requestAppointmentCancellation` tool.
* **`HospitalAgentService7.java` & `HospitalAgentController7.java` (`service`, `controller`):** Wires `ChatClient` with `HospitalGuardrailAdvisor4`, `MessageWindowChatMemory`, and HITL tools, exposing `/api/v7/agent/chat`, `/api/v7/approvals/pending`, and `/api/v7/approvals/{approvalId}/decide`.

---

## Verified Day 7 APIs, Requests & Responses

### 1. Check Initial Appointment Status (Before Cancellation)
Verifies that the seeded appointment (`APT-901`) is initially in the `CONFIRMED` state before any cancellation workflow starts.

* **Request:**
  `GET http://localhost:8080/api/v7/agent/chat?conversationId=701&message=What is the current status of APT-901?`
* **Response:**
```json
{
    "conversationId": "701",
    "response": "Appointment APT-901 for Harsha with Dr. Ananya Sharma (Cardiology on Monday) has current status: CONFIRMED"
}


2. Request Appointment Cancellation (Triggers HITL Gate)
When the patient asks to cancel APT-901, the agent invokes requestAppointmentCancellation. Instead of cancelling immediately, it generates an approval ticket (APR-C50695) and suspends execution until a supervisor reviews it.

Request:GET http://localhost:8080/api/v7/agent/chat?conversationId=701&message=Please cancel my appointment APT-901 because of an urgent work trip.

Response:
{
    "conversationId": "701",
    "response": "Your cancellation request for appointment APT-901 has been submitted for approval. Your Approval Ticket ID is APR-C50695. Please note that a Front-Desk Supervisor must approve this request before the cancellation takes effect."
}

3. Inspect Pending Supervisor Approval Queue
Allows a hospital supervisor to view all queued high-risk agent proposals awaiting human decision, including proposed state transitions (CONFIRMED → CANCELLED_APPROVED).

Request:GET http://localhost:8080/api/v7/approvals/pending

Response:
[
    {
        "approvalId": "APR-C50695",
        "actionType": "CANCEL_APPOINTMENT",
        "targetReferenceId": "APT-901",
        "requestedBy": "Harsha",
        "reason": "Urgent work trip",
        "proposedChanges": {
            "preferredDay": "Monday",
            "proposedStatus": "CANCELLED_APPROVED",
            "previousStatus": "CONFIRMED",
            "department": "Cardiology",
            "doctorName": "Dr. Ananya Sharma"
        },
        "status": "PENDING_APPROVAL",
        "reviewerNotes": null,
        "createdAt": "2026-11-25T02:58:32.622214800Z",
        "decidedAt": null
    }
]



4. Supervisor Decision (approved: true to Approve / false to Reject)
Commits or rejects the queued proposal deterministically in Java. Passing "approved": true finalizes the cancellation; passing "approved": false rejects the request and keeps the appointment active.

Request:POST http://localhost:8080/api/v7/approvals/APR-C50695/decide

Request Body:
{
  "approved": true,
  "reviewerName": "Supervisor Raju",
  "reviewerNotes": "Verified patient emergency request over phone."
}

Response:
{
    "approvalId": "APR-C50695",
    "actionType": "CANCEL_APPOINTMENT",
    "targetReferenceId": "APT-901",
    "requestedBy": "Harsha",
    "reason": "Urgent work trip",
    "proposedChanges": {
        "preferredDay": "Monday",
        "proposedStatus": "CANCELLED_APPROVED",
        "previousStatus": "CONFIRMED",
        "department": "Cardiology",
        "doctorName": "Dr. Ananya Sharma"
    },
    "status": "APPROVED",
    "reviewerNotes": "[APPROVED by Supervisor Raju] Verified patient emergency request over phone.",
    "createdAt": "2026-11-25T02:58:32.622214800Z",
    "decidedAt": "2026-11-25T03:01:15.104512Z"
}

5. Verify Appointment Status After Supervisor Approval
Confirms that the underlying appointment record has now transitioned to CANCELLED_APPROVED following supervisor sign-off.

Request:GET http://localhost:8080/api/v7/agent/chat?conversationId=701&message=Check status of APT-901 again

Response:
{
    "conversationId": "701",
    "response": "The status of APT-901 has been updated. The appointment has been cancelled and approved by the Front-Desk Supervisor."
}
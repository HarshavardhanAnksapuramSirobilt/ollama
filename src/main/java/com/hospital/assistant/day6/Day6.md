# Day 6: Tool / Function Calling (`@Tool` & Live Backend Integration)

## What Was Learned & Implemented
* **Controlled Backend Access via `@Tool` & `@ToolParam`:** Exposed Java methods in Spring Boot as JSON-schema tools so the LLM (`llama3.2:3b`) can dynamically look up live doctor schedules, create provisional bookings, and verify appointment statuses instead of guessing static facts.
* **Spring AI Tool Execution Loop:** Understood that the LLM never runs Java code directly—it outputs a structured tool call request, Spring AI's `ToolCallingManager` invokes the Java method, and the JSON return payload is sent back to the model to synthesize a grounded response.
* **Defense-in-Depth Tool Validation:** Enforced deterministic Java business rules inside write `@Tool` methods (verifying the doctor exists and works on the requested day) so the backend rejects invalid days and returns structured feedback that lets the LLM self-correct.
* **Stateful Multi-Turn Agent:** Combined `HospitalGuardrailAdvisor4`, `MessageWindowChatMemory` (`conversationId`), and `.defaultTools(...)` so follow-up turns remember context across read and write tool calls.

---

## Day 6 Files Overview
* **`DoctorInfo6.java` & `AppointmentSlot6.java` (`dto`):** Java 21 record DTOs representing live doctor availability/fees and provisional appointment booking details.
* **`HospitalOperationsTools6.java` (`tools`):** Spring `@Component` exposing `@Tool`-annotated methods (`findDoctorsByDepartment`, `bookProvisionalAppointment` with schedule validation, and `checkAppointmentStatus`).
* **`HospitalAgentService6.java` (`service`):** Configures `ChatClient` (`temperature: 0.1`) with `HospitalGuardrailAdvisor4`, `MessageChatMemoryAdvisor`, and `HospitalOperationsTools6`.
* **`HospitalAgentController6.java` (`controller`):** Exposes `GET /api/v6/agent/chat` for stateful multi-turn agentic conversations.

---

## Verified API Flows & Sample Responses

### 1. Read-Only Tool Lookup (`findDoctorsByDepartment`)
* **Request:**
  `GET http://localhost:8080/api/v6/agent/chat?conversationId=1&message=Give me doctors avialbilty for cardiology department`
* **Response:**
```json
{
    "conversationId": "1",
    "response": "The doctor availability for the Cardiology department is as follows:\n\n* Dr. Ananya Sharma is available on:\n\t+ Mondays\n\t+ Wednesdays\n\t+ Fridays\n\t+ Shift timing: 9:00 AM - 1:00 PM\n\t+ Consultation fee: ₹ 1200"
}


2. Multi-Turn Write Tool Execution (bookProvisionalAppointment)

Request:GET http://localhost:8080/api/v6/agent/chat?conversationId=1&message=book appointment on mondays  with Dr. Ananya Sharma on my name Harsha
Response:
{
    "conversationId": "1",
    "response": "Appointment booked successfully for Harsha with Dr. Ananya Sharma in the Cardiology department on Monday. The appointment timing is 9:00 AM - 1:00 PM and the consultation fee is ₹ 1200. You will receive a reference number APT-B3FFC7 for future reference."
}


3. Memory-Assisted Status Check (checkAppointmentStatus)

Request:GET http://localhost:8080/api/v6/agent/chat?conversationId=1&message=verify my status

Response:
{
    "conversationId": "1",
    "response": "Your appointment status is PROVISIONAL_CONFIRMED. This means that the appointment has been booked and confirmed, but it has not been finalized yet. You will receive a reminder closer to the appointment date and time."
}
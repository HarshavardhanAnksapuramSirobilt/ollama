package com.hospital.assistant.day7.tools;

import com.hospital.assistant.day6.dto.AppointmentSlot6;
import com.hospital.assistant.day6.dto.DoctorInfo6;
import com.hospital.assistant.day7.dto.PendingApprovalAction7;
import com.hospital.assistant.day7.service.HumanApprovalGateService7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class HospitalHitlTools7 {

    private static final Logger log = LoggerFactory.getLogger(HospitalHitlTools7.class);

    private final HumanApprovalGateService7 approvalGateService7;

    private final List<DoctorInfo6> doctorRoster = List.of(
            new DoctorInfo6("DOC-101", "Dr. Ananya Sharma", "Cardiology", List.of("Monday", "Wednesday", "Friday"), "09:00 AM - 01:00 PM", 1200),
            new DoctorInfo6("DOC-102", "Dr. Vikram Deshmukh", "Orthopedics", List.of("Tuesday", "Thursday", "Saturday"), "10:00 AM - 02:00 PM", 900),
            new DoctorInfo6("DOC-103", "Dr. Meera Nair", "General Medicine", List.of("Monday", "Tuesday", "Thursday", "Friday"), "09:30 AM - 04:30 PM", 600)
    );

    public HospitalHitlTools7(HumanApprovalGateService7 approvalGateService7) {
        this.approvalGateService7 = approvalGateService7;
    }

    @Tool(description = "Search available doctors by department (Cardiology, Orthopedics, General Medicine).")
    public List<DoctorInfo6> findDoctorsByDepartment(
            @ToolParam(description = "Department name") String department
    ) {
        log.info("TOOL EXECUTED -> findDoctorsByDepartment(department='{}')", department);
        if (department == null || department.isBlank()) return doctorRoster;
        return doctorRoster.stream()
                .filter(d -> d.department().equalsIgnoreCase(department.trim()))
                .toList();
    }

    @Tool(description = "Book a provisional outpatient consultation appointment after validating doctor availability.")
    public String bookProvisionalAppointment(
            @ToolParam(description = "Full name of the patient") String patientName,
            @ToolParam(description = "Full name of the doctor") String doctorName,
            @ToolParam(description = "Department name") String department,
            @ToolParam(description = "Preferred day of the week") String preferredDay
    ) {
        DoctorInfo6 matchedDoctor = doctorRoster.stream()
                .filter(d -> d.name().equalsIgnoreCase(doctorName.trim()))
                .findFirst()
                .orElse(null);

        if (matchedDoctor == null) {
            return "BOOKING_FAILED: Doctor '" + doctorName + "' not found.";
        }
        boolean available = matchedDoctor.availableDays().stream()
                .anyMatch(day -> day.equalsIgnoreCase(preferredDay.trim()));
        if (!available) {
            return "BOOKING_FAILED: " + matchedDoctor.name() + " is only available on " + matchedDoctor.availableDays();
        }

        String refId = "APT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        AppointmentSlot6 slot = new AppointmentSlot6(
                refId, patientName, matchedDoctor.name(), matchedDoctor.department(),
                preferredDay.trim(), "PROVISIONAL_CONFIRMED"
        );
        approvalGateService7.saveProvisionalAppointment(slot);
        return "BOOKING_CONFIRMED: Reference=" + refId + ", Status=PROVISIONAL_CONFIRMED";
    }

    @Tool(description = "Check the current status of an appointment by its booking reference ID (e.g., APT-901).")
    public String checkAppointmentStatus(
            @ToolParam(description = "Appointment booking reference starting with APT-") String bookingReference
    ) {
        log.info("TOOL EXECUTED -> checkAppointmentStatus(ref='{}')", bookingReference);
        AppointmentSlot6 slot = approvalGateService7.getAppointment(bookingReference);
        if (slot == null) {
            return "NOT_FOUND: No appointment found with reference " + bookingReference;
        }
        return String.format("Appointment %s for %s with %s (%s on %s) has current status: %s",
                slot.bookingReference(), slot.patientName(), slot.doctorName(),
                slot.department(), slot.preferredDay(), slot.status());
    }

    @Tool(description = "Submit a cancellation request for an existing appointment. Because cancelling appointments is a high-risk action, this queues the request for mandatory Human-in-the-Loop supervisor approval rather than cancelling immediately.")
    public String requestAppointmentCancellation(
            @ToolParam(description = "The booking reference code to cancel (e.g., APT-901)") String bookingReference,
            @ToolParam(description = "Reason provided by the patient for cancelling the appointment") String cancellationReason
    ) {
        log.info("TOOL EXECUTED -> requestAppointmentCancellation(ref='{}', reason='{}')",
                bookingReference, cancellationReason);

        AppointmentSlot6 existing = approvalGateService7.getAppointment(bookingReference);
        if (existing == null) {
            return "CANCELLATION_FAILED: No appointment found with reference " + bookingReference;
        }
        if ("CANCELLED_APPROVED".equals(existing.status())) {
            return "CANCELLATION_SKIPPED: Appointment " + bookingReference + " is already cancelled.";
        }

        PendingApprovalAction7 proposal = approvalGateService7.queueForApproval(
                "CANCEL_APPOINTMENT",
                existing.bookingReference(),
                existing.patientName(),
                cancellationReason,
                Map.of(
                        "doctorName", existing.doctorName(),
                        "department", existing.department(),
                        "preferredDay", existing.preferredDay(),
                        "previousStatus", existing.status(),
                        "proposedStatus", "CANCELLED_APPROVED"
                )
        );

        return String.format(
                "PENDING_HUMAN_APPROVAL: Cancellation for %s has NOT been finalized yet. " +
                        "Approval Ticket ID is %s. Inform the patient that a Front-Desk Supervisor must approve %s before the cancellation takes effect.",
                existing.bookingReference(), proposal.approvalId(), proposal.approvalId()
        );
    }
}
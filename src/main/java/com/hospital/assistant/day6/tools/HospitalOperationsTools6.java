package com.hospital.assistant.day6.tools;

import com.hospital.assistant.day6.dto.AppointmentSlot6;
import com.hospital.assistant.day6.dto.DoctorInfo6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HospitalOperationsTools6 {

    private static final Logger log = LoggerFactory.getLogger(HospitalOperationsTools6.class);

    // Simulated live hospital roster (can be backed by Spring Data JPA repository)
    private final List<DoctorInfo6> doctorRoster = List.of(
            new DoctorInfo6("DOC-101", "Dr. Ananya Sharma", "Cardiology", List.of("Monday", "Wednesday", "Friday"), "09:00 AM - 01:00 PM", 1200),
            new DoctorInfo6("DOC-102", "Dr. Vikram Deshmukh", "Orthopedics", List.of("Tuesday", "Thursday", "Saturday"), "10:00 AM - 02:00 PM", 900),
            new DoctorInfo6("DOC-103", "Dr. Meera Nair", "General Medicine", List.of("Monday", "Tuesday", "Thursday", "Friday"), "09:30 AM - 04:30 PM", 600),
            new DoctorInfo6("DOC-104", "Dr. Rohan Kulkarni", "Neurology", List.of("Wednesday", "Saturday"), "11:00 AM - 03:00 PM", 1500)
    );

    private final Map<String, AppointmentSlot6> appointments = new ConcurrentHashMap<>();

    @Tool(description = "Search available hospital doctors by medical department (e.g., Cardiology, Orthopedics, General Medicine, Neurology). Returns doctor names, available days, timings, and consultation fees in INR.")
    public List<DoctorInfo6> findDoctorsByDepartment(
            @ToolParam(description = "The hospital department name such as Cardiology, Orthopedics, General Medicine, or Neurology") String department
    ) {
        log.info("TOOL EXECUTED -> findDoctorsByDepartment(department='{}')", department);
        if (department == null || department.isBlank()) {
            return doctorRoster;
        }
        return doctorRoster.stream()
                .filter(doc -> doc.department().equalsIgnoreCase(department.trim()))
                .toList();
    }

//    @Tool(description = "Schedule a provisional outpatient consultation appointment with a specific doctor for a patient. Requires patient name, doctor name, department, and preferred day of the week.")
//    public AppointmentSlot6 bookProvisionalAppointment(
//            @ToolParam(description = "Full name of the patient") String patientName,
//            @ToolParam(description = "Full name of the doctor (e.g., Dr. Ananya Sharma)") String doctorName,
//            @ToolParam(description = "Department name (e.g., Cardiology)") String department,
//            @ToolParam(description = "Preferred day of the week (e.g., Monday)") String preferredDay
//    ) {
//        log.info("TOOL EXECUTED -> bookProvisionalAppointment(patient='{}', doctor='{}', day='{}')",
//                patientName, doctorName, preferredDay);
//
//        String refId = "APT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
//        AppointmentSlot6 slot = new AppointmentSlot6(
//                refId,
//                patientName,
//                doctorName,
//                department,
//                preferredDay,
//                "PROVISIONAL_CONFIRMED"
//        );
//        appointments.put(refId, slot);
//        return slot;
//    }

    /* Adding Provisonal appontment booking with validating doctor availability on that particular day */
    @Tool(description = "Schedule a provisional outpatient consultation appointment with a specific doctor for a patient. Requires patient name, doctor name, department, and preferred day of the week.")
    public String bookProvisionalAppointment(
            @ToolParam(description = "Full name of the patient") String patientName,
            @ToolParam(description = "Full name of the doctor (e.g., Dr. Ananya Sharma)") String doctorName,
            @ToolParam(description = "Department name (e.g., Cardiology)") String department,
            @ToolParam(description = "Preferred day of the week (e.g., Monday)") String preferredDay
    ) {
        log.info("TOOL EXECUTED -> bookProvisionalAppointment(patient='{}', doctor='{}', day='{}')",
                patientName, doctorName, preferredDay);

        // 1. Validate that the doctor exists in the roster
        DoctorInfo6 matchedDoctor = doctorRoster.stream()
                .filter(doc -> doc.name().equalsIgnoreCase(doctorName.trim()))
                .findFirst()
                .orElse(null);

        if (matchedDoctor == null) {
            return "BOOKING_FAILED: Doctor '" + doctorName + "' was not found in the hospital roster.";
        }

        // 2. Validate that the doctor actually works on the requested day
        boolean isAvailableOnDay = matchedDoctor.availableDays().stream()
                .anyMatch(day -> day.equalsIgnoreCase(preferredDay.trim()));

        if (!isAvailableOnDay) {
            return String.format(
                    "BOOKING_FAILED: %s (%s) is NOT available on %s. Available days are: %s (%s). Ask the patient to pick a valid day.",
                    matchedDoctor.name(),
                    matchedDoctor.department(),
                    preferredDay,
                    matchedDoctor.availableDays(),
                    matchedDoctor.shiftTiming()
            );
        }

        // 3. Valid request -> Create provisional booking
        String refId = "APT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        AppointmentSlot6 slot = new AppointmentSlot6(
                refId,
                patientName,
                matchedDoctor.name(),
                matchedDoctor.department(),
                preferredDay.trim(),
                "PROVISIONAL_CONFIRMED"
        );
        appointments.put(refId, slot);

        return String.format(
                "BOOKING_CONFIRMED: Reference=%s, Patient=%s, Doctor=%s, Department=%s, Day=%s, Timing=%s, Fee=₹%d",
                slot.bookingReference(), slot.patientName(), slot.doctorName(),
                slot.department(), slot.preferredDay(), matchedDoctor.shiftTiming(), matchedDoctor.consultationFeeInr()
        );
    }

    @Tool(description = "Look up an existing appointment status using its booking reference ID (e.g., APT-1A2B3C).")
    public AppointmentSlot6 checkAppointmentStatus(
            @ToolParam(description = "The appointment booking reference code starting with APT-") String bookingReference
    ) {
        log.info("TOOL EXECUTED -> checkAppointmentStatus(ref='{}')", bookingReference);
        if (bookingReference == null) {
            return null;
        }
        return appointments.get(bookingReference.trim().toUpperCase());
    }
}
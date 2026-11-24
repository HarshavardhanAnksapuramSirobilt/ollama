package com.hospital.assistant.day6.dto;

public record AppointmentSlot6(
        String bookingReference,
        String patientName,
        String doctorName,
        String department,
        String preferredDay,
        String status
) {}
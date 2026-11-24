package com.hospital.assistant.day6.dto;

import java.util.List;

public record DoctorInfo6(
        String doctorId,
        String name,
        String department,
        List<String> availableDays,
        String shiftTiming,
        int consultationFeeInr
) {}
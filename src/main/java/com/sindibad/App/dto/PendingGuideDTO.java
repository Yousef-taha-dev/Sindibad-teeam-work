package com.sindibad.App.dto;

import java.time.LocalTime;

public record PendingGuideDTO(
        Integer guideId,
        Integer personId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String bio,
        Integer experienceYears,
        String availableDays,
        LocalTime startTime,
        LocalTime endTime,
        String approvalStatus
) {}
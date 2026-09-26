package com.sindibad.App.dto;

import java.time.LocalDate;

public record PersonResponseDTO(
        Long personId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String gender,
        LocalDate dateOfBirth,
        String nationality,
        String profileImage,
        String accountStatus
) {}
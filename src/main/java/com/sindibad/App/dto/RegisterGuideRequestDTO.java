package com.sindibad.App.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterGuideRequestDTO(
        @Valid
        @NotNull(message = "Personal information is required")
        RegisterRequestDTO personalInfo,

        @NotBlank(message = "Bio is required")
        @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
        String bio,

        @NotNull(message = "Experience years is required")
        @Min(value = 0, message = "Experience years cannot be negative")
        Integer experienceYears
) {
}
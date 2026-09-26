package com.sindibad.App.dto;

public record AuthResponseDTO(
        String token,
        String role,
        PersonResponseDTO user
) {}
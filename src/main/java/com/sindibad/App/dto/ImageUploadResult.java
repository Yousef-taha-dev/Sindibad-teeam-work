package com.sindibad.App.dto;

public record ImageUploadResult(
        String secureUrl,
        String publicId
) {
}
package com.sindibad.App.dto;

public record NotificationResponseDTO(
        Long notificationId,
        Long personId,
        String title,
        String message,
        String notificationType
) {
}
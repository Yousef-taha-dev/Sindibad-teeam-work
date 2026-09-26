package com.sindibad.App.dto;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long notificationId,
        Long personId,
        String title,
        String message,
        String notificationType,
        Boolean isRead,
        LocalDateTime createdAt
) {}
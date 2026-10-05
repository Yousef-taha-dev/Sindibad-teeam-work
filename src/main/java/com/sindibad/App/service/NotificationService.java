package com.sindibad.App.service;

import com.sindibad.App.dto.NotificationResponseDTO;

import java.util.List;

public interface NotificationService {

    List<NotificationResponseDTO> getMyNotifications(String email);

    void deleteMyNotification(Long notificationId, String email);

    void createNotification(
            Long personId,
            String title,
            String message,
            String notificationType
    );
}
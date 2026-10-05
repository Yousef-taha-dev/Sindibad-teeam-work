package com.sindibad.App.controller;

import com.sindibad.App.dto.NotificationResponseDTO;
import com.sindibad.App.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(
        name = "Notifications",
        description = "Endpoints for managing user notifications"
)
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ==================== Get My Notifications ====================

    @Operation(
            summary = "Get my notifications",
            description = "Returns notifications belonging to the currently authenticated user."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('TOURIST', 'GUIDE', 'ADMIN')")
    public ResponseEntity<List<NotificationResponseDTO>> getMyNotifications(
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                notificationService.getMyNotifications(email)
        );
    }

    // ==================== Delete My Notification ====================

    @Operation(
            summary = "Delete my notification",
            description = "Deletes a notification belonging to the currently authenticated user."
    )
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('TOURIST', 'GUIDE', 'ADMIN')")
    public ResponseEntity<Void> deleteMyNotification(
            @PathVariable Long notificationId,
            Authentication authentication) {

        String email = authentication.getName();

        notificationService.deleteMyNotification(
                notificationId,
                email
        );

        return ResponseEntity.noContent().build();
    }
}
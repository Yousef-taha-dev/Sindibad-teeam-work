package com.sindibad.App.controller;

import com.sindibad.App.dto.EarningResponseDTO;
import com.sindibad.App.service.EarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/earnings")
@Tag(
        name = "Earnings",
        description = "Endpoints for managing guide and admin earnings"
)
@SecurityRequirement(name = "bearerAuth")
public class EarningController {

    private final EarningService earningService;

    public EarningController(EarningService earningService) {
        this.earningService = earningService;
    }

    // ==================== Get All Earnings ====================

    @Operation(
            summary = "Get all earnings",
            description = "Returns all earnings generated from completed payments."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EarningResponseDTO>> getAllEarnings() {

        return ResponseEntity.ok(
                earningService.getAllEarnings()
        );
    }

    // ==================== Get Guide Earnings ====================

    @Operation(
            summary = "Get earnings by guide",
            description = "Returns all earnings belonging to a specific guide."
    )
    @GetMapping("/guide/{guideId}")
    @PreAuthorize("hasAnyRole('GUIDE', 'ADMIN')")
    public ResponseEntity<List<EarningResponseDTO>> getEarningsByGuideId(
            @PathVariable Long guideId,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException(
                    "You can only view your own earnings."
            );
        }

        return ResponseEntity.ok(
                earningService.getEarningsByGuideId(
                        guideId,
                        authentication.getName(),
                        isAdmin(authentication)
                )
        );
    }

    // ==================== Get Total Guide Earnings ====================

    @Operation(
            summary = "Get total earnings for a guide",
            description = "Returns the total accumulated earnings amount for a specific guide."
    )
    @GetMapping("/guide/{guideId}/total")
    @PreAuthorize("hasAnyRole('GUIDE', 'ADMIN')")
    public ResponseEntity<BigDecimal> getTotalEarningsByGuideId(
            @PathVariable Long guideId,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException(
                    "You can only view your own earnings."
            );
        }

        return ResponseEntity.ok(
                earningService.getTotalEarningsByGuideId(
                        guideId,
                        authentication.getName(),
                        isAdmin(authentication)
                )
        );
    }

    private boolean isAdmin(Authentication authentication) {

        if (authentication.getAuthorities() == null) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }
}
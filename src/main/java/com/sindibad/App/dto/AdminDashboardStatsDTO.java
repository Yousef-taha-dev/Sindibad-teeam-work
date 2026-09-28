package com.sindibad.App.dto;

import java.math.BigDecimal;

public record AdminDashboardStatsDTO(
        long totalPersons,
        long totalTourists,
        long totalGuides,
        long pendingGuideApprovals,
        long totalTrips,
        long totalBookings,
        BigDecimal totalAdminEarnings
) {}

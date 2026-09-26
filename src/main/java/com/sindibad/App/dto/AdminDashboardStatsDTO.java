package com.sindibad.App.dto;

import java.math.BigDecimal;

public record AdminDashboardStatsDTO(
        long totalUsers,
        long totalGuides,
        long pendingGuides,
        long totalTrips,
        long totalBookings,
        BigDecimal totalSystemRevenue,
        BigDecimal totalGrossSales
) {}
package com.squad6.deneasybot.model;

import java.util.List;

public record DashboardMetricsDTO(
        long totalCompanies,
        long totalUsers,
        long totalFeedbacks,
        Double averageRating,
        List<ChartDataDTO> attendanceHistory
) {
}
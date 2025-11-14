package com.squad6.deneasybot.model;


public record DashboardMetricsDTO(
        long totalCompanies,
        long totalUsers,
        long totalFeedbacks,
        Double averageRating
) {
}
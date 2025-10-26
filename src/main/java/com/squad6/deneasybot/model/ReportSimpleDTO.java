package com.squad6.deneasybot.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportSimpleDTO(
        String reportType,
        String companyName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal operationalRevenue,
        BigDecimal variableCosts,
        BigDecimal fixedExpenses,
        BigDecimal operationalResult
) {
}
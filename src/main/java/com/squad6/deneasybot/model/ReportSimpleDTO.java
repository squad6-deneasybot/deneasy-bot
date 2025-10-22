package com.squad6.deneasybot.model;

import java.math.BigDecimal;

public record ReportSimpleDTO(
        String reportType,
        String datas,
        BigDecimal operatingRevenue,
        BigDecimal variableCosts,
        BigDecimal fixedExpenses,
        BigDecimal operatingResult
) {
}
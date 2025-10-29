package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ReportService {

    public ReportSimpleDTO getSimpleReport(Long companyId) {
        String reportType = "Simples";
        String companyName = "Empresa";
        LocalDate startDate = LocalDate.of(2025, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 10, 21);
        BigDecimal operationalRevenue = new BigDecimal("150000.00");
        BigDecimal variableCosts = new BigDecimal("45000.00");
        BigDecimal fixedExpenses = new BigDecimal("30000.00");
        BigDecimal operationalResult = new BigDecimal("75000.00");

        return new ReportSimpleDTO(
                reportType,
                companyName,
                startDate,
                endDate,
                operationalRevenue,
                variableCosts,
                fixedExpenses,
                operationalResult
        );
    }
}
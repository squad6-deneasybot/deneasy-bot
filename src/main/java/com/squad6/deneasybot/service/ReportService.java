package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ReportService {

    public ReportSimpleDTO getSimpleReport(Long companyId) {
        // Dados fixos (mockados)
        String reportType = "Relatório Simples";
        String datas = "01/10/2025 - 21/10/2025";
        BigDecimal operatingRevenue = new BigDecimal("150000.00");
        BigDecimal variableCosts = new BigDecimal("45000.00");
        BigDecimal fixedExpenses = new BigDecimal("30000.00");
        BigDecimal operatingResult = new BigDecimal("75000.00");

        return new ReportSimpleDTO(
                reportType,
                datas,
                operatingRevenue,
                variableCosts,
                fixedExpenses,
                operatingResult
        );
    }
}
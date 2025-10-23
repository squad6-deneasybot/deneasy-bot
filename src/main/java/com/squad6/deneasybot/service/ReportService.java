package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    /**
     * STUB (Mock) para RF-REPORT-01 (Dependência da Dupla 3).
     * Simula a busca do relatório (mockado).
     */
    public ReportSimpleDTO getSimpleReport(Long companyId) {
        logger.warn("STUB (ReportService): Método 'getSimpleReport' chamado para o companyId {}. Retornando dados mockados.", companyId);

        // Retorna o DTO mockado que o WhatsAppFormatterService espera
        return new ReportSimpleDTO(
                "SIMPLE_MOCK",
                "Mock Company (Stub)",
                LocalDate.now().minusDays(30),
                LocalDate.now(),
                new BigDecimal("10000.00"), // operationalRevenue
                new BigDecimal("2000.00"),  // variableCosts
                new BigDecimal("3000.00"),  // fixedExpenses
                new BigDecimal("5000.00")   // operationalResult
        );
    }
}
package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final FinancialAggregatorService financialAggregatorService;

    @Autowired
    public ReportService(FinancialAggregatorService financialAggregatorService) {
        this.financialAggregatorService = financialAggregatorService;
    }

    public ReportSimpleDTO generateSimpleReport(String appKey, String appSecret, String period) {
        return financialAggregatorService.aggregateReportData(appKey, appSecret, period);
    }
}
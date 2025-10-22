package com.squad6.deneasybot.controller;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import com.squad6.deneasybot.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }
    
    @GetMapping("/simple")
    public ResponseEntity<ReportSimpleDTO> getSimpleReport(@RequestParam(defaultValue = "1") Long companyId) {
        ReportSimpleDTO report = reportService.getSimpleReport(companyId);
        return ResponseEntity.ok(report);
    }
}
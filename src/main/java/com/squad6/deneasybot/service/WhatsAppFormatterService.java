package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class WhatsAppFormatterService {

    public String formatSimpleReport(ReportSimpleDTO dto) {
        StringBuilder sb = new StringBuilder();

        sb.append("📊 *").append(dto.reportType()).append("*\n");
        sb.append("🗓️ *Período:* ").append(dto.dates()).append("\n\n");

        sb.append("💰 *Receita Operacional:* ").append(formatCurrency(dto.operatingRevenue())).append("\n");
        sb.append("💸 *Custos Variáveis:* ").append(formatCurrency(dto.variableCosts())).append("\n");
        sb.append("🏠 *Despesas Fixas:* ").append(formatCurrency(dto.fixedExpenses())).append("\n\n");
        sb.append("✅ *Resultado Operacional:* ").append(formatCurrency(dto.operatingResult())).append("\n");

        return sb.toString();
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return currencyFormatter.format(value);
    }
}
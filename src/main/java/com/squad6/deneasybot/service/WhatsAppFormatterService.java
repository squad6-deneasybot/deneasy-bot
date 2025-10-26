package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import com.squad6.deneasybot.model.UserDTO;
import com.squad6.deneasybot.model.UserProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class WhatsAppFormatterService {

    // Método atualizado para RF-REPORT-01
    public String formatSimpleReport(ReportSimpleDTO dto) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        sb.append("🏢 *Empresa:* ").append(dto.companyName()).append("\n");
        sb.append("📊 *").append(dto.reportType()).append("*\n");
        sb.append("🗓️ *Período:* ").append(dto.startDate().format(dtf)).append(" a ").append(dto.endDate().format(dtf)).append("\n\n");

        sb.append("💰 *Receita Operacional:* ").append(formatCurrency(dto.operationalRevenue())).append("\n");
        sb.append("💸 *Custos Variáveis:* ").append(formatCurrency(dto.variableCosts())).append("\n");
        sb.append("🏠 *Despesas Fixas:* ").append(formatCurrency(dto.fixedExpenses())).append("\n\n");
        sb.append("✅ *Resultado Operacional:* ").append(formatCurrency(dto.operationalResult())).append("\n");

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
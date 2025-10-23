package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import com.squad6.deneasybot.model.UserDTO;
import com.squad6.deneasybot.model.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppFormatterService {

    public String formatMenu(UserProfile profile) {
        StringBuilder menu = new StringBuilder();
        menu.append("Olá! 👋 Escolha uma das opções abaixo:\n\n");
        menu.append("1️⃣ Pedir Relatório Simples\n");
        menu.append("2️⃣ Perguntas Frequentes (FAQ)\n");

        if (profile == UserProfile.MANAGER) {
            menu.append("3️⃣ Gerenciar Funcionários\n");
        }

        menu.append("-----------------------------\n");
        menu.append("9️⃣ Falar com um humano");

        return menu.toString();
    }

    public String formatSimpleReport(ReportSimpleDTO dto) {

        String revenue = String.format("%,.2f", dto.operationalRevenue());
        String costs = String.format("%,.2f", dto.variableCosts());
        String expenses = String.format("%,.2f", dto.fixedExpenses());
        String result = String.format("%,.2f", dto.operationalResult());

        return "📊 *Relatório Simplificado (" + dto.reportType() + ")* 📊\n\n" +
                "Empresa: " + dto.companyName() + "\n" +
                "Período: " + dto.startDate() + " a " + dto.endDate() + "\n\n" +
                "-----------------------------------\n" +
                "🟢 Receita Operacional: R$ " + revenue + "\n" +
                "🟠 Custos Variáveis: R$ " + costs + "\n" +
                "🔴 Despesas Fixas: R$ " + expenses + "\n" +
                "-----------------------------------\n" +
                "🔵 *Resultado Operacional: R$ " + result + "*";
    }

    public String formatFallbackError() {
        return "😕 Desculpe, não entendi o que você quis dizer. Tente novamente ou digite *Menu*.";
    }

    public String formatUserCreated(UserDTO newUser) {
        return "✅ Usuário *" + newUser.getName() + "* foi criado com sucesso!";
    }
}

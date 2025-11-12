package com.squad6.deneasybot.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import com.squad6.deneasybot.model.UserDTO;
import com.squad6.deneasybot.model.UserProfile;
import com.squad6.deneasybot.model.CategoryStat;

@Service
public class WhatsAppFormatterService {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String formatMenu(UserProfile profile) {
        StringBuilder menu = new StringBuilder();
        menu.append("Olá! 👋 Escolha uma das opções abaixo:\n\n");
        menu.append("1️⃣ Pedir Relatório Simples\n");
        menu.append("2️⃣ Perguntas Frequentes (FAQ)\n");
        menu.append("3️⃣ Falar com um humano\n");

        if (profile == UserProfile.MANAGER) {
            menu.append("4️⃣ Gerenciar Funcionários\n");
        }

        menu.append("5️⃣ Sugerir melhoria\n");

        return menu.toString();
    }

    public String formatSimpleReport(ReportSimpleDTO dto) {

        String revenue = formatCurrency(dto.operationalRevenue());
        String costs = formatCurrency(dto.variableCosts());
        String expenses = formatCurrency(dto.fixedExpenses());
        String result = formatCurrency(dto.operationalResult());

        String startDateStr = (dto.startDate() != null) ? dto.startDate().format(DATE_FORMATTER) : "N/A";
        String endDateStr = (dto.endDate() != null) ? dto.endDate().format(DATE_FORMATTER) : "N/A";

        return "📊 *Relatório " + dto.reportType() + "* \n\n" +
                "Empresa: " + dto.companyName() + "\n" +
                "Período: " + startDateStr + " a " + endDateStr + "\n\n" +
                "🟢 Receita Operacional: " + revenue + "\n" +
                "🟠 Custos Variáveis: " + costs + "\n" +
                "🔴 Despesas Fixas: " + expenses + "\n" +
                "🔵 *Resultado Operacional: " + result + "*";
    }

    public String formatFaqProjecaoCaixa(BigDecimal saldoAtual, BigDecimal totalPagar, BigDecimal totalReceber,
                                         BigDecimal saldoPrevisto, int dias) {

        String sAtual = String.format("%,.2f", saldoAtual);
        String sPagar = String.format("%,.2f", totalPagar);
        String sReceber = String.format("%,.2f", totalReceber);
        String sPrevisto = String.format("%,.2f", saldoPrevisto);

        return "🔮 *Projeção de Caixa (Próximos " + dias + " dias)*\n\n" + "🔵 Saldo Atual: R$ " + sAtual + "\n"
                + "🟢 Prev. Receber: R$ " + sReceber + "\n" + "🔴 Prev. Pagar: R$ " + sPagar + "\n\n"
                + "Saldo Previsto: *R$ " + sPrevisto + "*";
    }

    public String formatFallbackError() {
        return "😕 Desculpe, não entendi o que você quis dizer. Tente novamente ou digite *Menu*.";
    }

    public String formatUserCreated(UserDTO newUser) {
        return "✅ Usuário *" + newUser.getName() + "* foi criado com sucesso!";
    }

    public String formatPostActionMenu() {
        return """
                O que você gostaria de fazer agora?
                
                1️⃣ Voltar ao Menu Principal
                2️⃣ Falar com um Atendente
                3️⃣ Encerrar Atendimento""";
    }

    public String formatFaqTitulosEmAtraso(long count1_30, BigDecimal total1_30, long count31_60, BigDecimal total31_60,
                                           long count61_90, BigDecimal total61_90, long count90_plus, BigDecimal total90_plus) {

        long totalCount = count1_30 + count31_60 + count61_90 + count90_plus;
        BigDecimal totalValue = total1_30.add(total31_60).add(total61_90).add(total90_plus);

        if (totalCount == 0) {
            return "Parabéns! Você não possui títulos de pagamento em atraso.";
        }

        String formattedTotalValue = String.format("%,.2f", totalValue);
        String formattedTotal1_30 = String.format("%,.2f", total1_30);
        String formattedTotal31_60 = String.format("%,.2f", total31_60);
        String formattedTotal61_90 = String.format("%,.2f", total61_90);
        String formattedTotal90_plus = String.format("%,.2f", total90_plus);

        return "Você possui *" + totalCount + "* títulos de pagamento em atraso, totalizando *R$ " + formattedTotalValue
                + "*.\n\n" + "Distribuição por faixa (Aging):\n" + "• 1 a 30 dias: " + count1_30 + " títulos (R$ "
                + formattedTotal1_30 + ")\n" + "• 31 a 60 dias: " + count31_60 + " títulos (R$ " + formattedTotal31_60
                + ")\n" + "• 61 a 90 dias: " + count61_90 + " títulos (R$ " + formattedTotal61_90 + ")\n"
                + "• Mais de 90 dias: " + count90_plus + " títulos (R$ " + formattedTotal90_plus + ")";
    }

    public String formatFaqTopCategorias(List<CategoryStat> topCategories) {
        if (topCategories == null || topCategories.isEmpty()) {
            return "Não localizamos nenhuma despesa paga nos últimos 30 dias.";
        }

        StringBuilder response = new StringBuilder(
                "Aqui estão seus principais geradores de despesa nos últimos 30 dias:\n\n");

        String[] emojis = { "🥇 1.", "🥈 2.", "🥉 3." };

        for (int i = 0; i < topCategories.size(); i++) {
            CategoryStat stat = topCategories.get(i);
            String formattedValue = formatCurrency(stat.totalValue());
            String categoryName = stat.categoryName();

            response.append(emojis[i])
                    .append(" ")
                    .append(categoryName)
                    .append(" (R$ ")
                    .append(formattedValue)
                    .append(")\n");
        }

        return response.toString().trim();
    }

    public String formatFaqTitulosAVencer(int countPagar, BigDecimal totalPagar, int countReceber, BigDecimal totalReceber, int days) {
        if (countPagar == 0 && countReceber == 0) {
            return "Boas notícias! Você não possui títulos a pagar ou a receber nos próximos " + days + " dias.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Nos próximos *").append(days).append(" dias*, você tem:\n\n");

        if (countPagar > 0) {
            sb.append("🔴 *A Pagar:* ").append(countPagar).append(" títulos, totalizando ")
                    .append(formatCurrency(totalPagar)).append(".\n");
        }

        if (countReceber > 0) {
            sb.append("🟢 *A Receber:* ").append(countReceber).append(" títulos, totalizando ")
                    .append(formatCurrency(totalReceber)).append(".");
        }

        return sb.toString().trim();
    }

    public String formatCrudMenu() {
        return """
                Gerenciamento de Funcionários:
                
                1️⃣ Listar todos
                2️⃣ Adicionar novo
                3️⃣ Atualizar um
                4️⃣ Remover um
                
                V. Voltar ao menu""";
    }

    public String formatEmployeeList(List<UserDTO> employees) {
        if (employees == null || employees.isEmpty()) {
            return "Você ainda não possui funcionários cadastrados.";
        }

        StringBuilder sb = new StringBuilder("Aqui estão seus funcionários:\n");
        for (UserDTO employee : employees) {
            sb.append("\n• ").append(employee.getName()).append(" (").append(employee.getEmail()).append(")");
        }
        return sb.toString();
    }

    public String formatEmployeeAdded(UserDTO employee) {
        return "✅ Funcionário *" + employee.getName() + "* adicionado com sucesso.";
    }

    public String formatEmployeeRemoved(String employeeName) {
        return "🗑️ Funcionário *" + employeeName + "* removido com sucesso.";
    }

    public String formatEmployeeUpdateSelector(List<UserDTO> employees) {
        if (employees == null || employees.isEmpty()) {
            return "Você não possui funcionários para atualizar.";
        }
        return formatEmployeeList(employees) + "\n\nDigite o e-mail do funcionário que deseja atualizar (ou 'V' para voltar):";
    }

    public String formatEmployeeUpdateFieldMenu(UserDTO employee) {
        return "O que você deseja atualizar para *" + employee.getName() + "*?\n\n" +
                "1️⃣ Nome\n" +
                "2️⃣ E-mail\n" +
                "3️⃣ Telefone\n\n" +
                "V. Cancelar";
    }

    public String formatCrudPostActionMenu() {
        return """
                O que você gostaria de fazer agora?
                
                1️⃣ Voltar ao menu de gerenciar funcionários
                2️⃣ Voltar ao menu principal
                3️⃣ Encerrar atendimento""";
    }

    public String formatWishlistPrompt() {
        return "Entendido! Sua opinião é muito importante para nós. 💡\n\n" +
                "Por favor, descreva em *uma única mensagem* o que você gostaria de ver " +
                "no DeneasyBot que facilitaria seu trabalho:";
    }

    public String formatWishlistThanks() {
        return "Obrigado! Sua sugestão foi registrada e será analisada pela nossa equipe. 👍";
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(PT_BR);
        return currencyFormatter.format(value);
    }
}
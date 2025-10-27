package com.squad6.deneasybot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);

    /**
     * STUB (Mock) para RF-MENU-01 (Dependência da Dupla 3).
     * Simula o processamento de uma mensagem no menu principal.
     */
    public String processMessage(String userPhone, String messageText) {
        logger.warn("STUB (MenuService): Método 'processMessage' chamado com: {}", messageText);

        // O Orchestrator (no meu exemplo) apenas pega a resposta e envia.
        // A Dupla 3 fará a lógica real (chamar ReportService, FAQService, etc.)

        if ("1".equals(messageText)) {
            // No futuro, a Dupla 3 fará este serviço chamar o 'reportService.getRelatorioSimples()'
            // e o 'formatterService.formatSimpleReport()'.
            return "STUB: Você pediu o Relatório Simples. (Lógica da Dupla 3 virá aqui)";
        }

        return "STUB: Menu Principal (Autenticado). Opção '" + messageText + "' inválida.";
    }
}
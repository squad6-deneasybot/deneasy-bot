package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);
    private final ReportService reportService;
    private final WhatsAppFormatterService whatsAppFormatterService;

    public MenuService(ReportService reportService, WhatsAppFormatterService whatsAppFormatterService) {
        this.reportService = reportService;
        this.whatsAppFormatterService = whatsAppFormatterService;
    }

    public String processMessage(String userPhone, String messageText) {
        logger.warn("STUB (MenuService): Método 'processMessage' chamado com: {}", messageText);

        if ("1".equals(messageText)) {
            ReportSimpleDTO report = reportService.getSimpleReport(1L);
            return whatsAppFormatterService.formatSimpleReport(report);
        }

        if ("2".equals(messageText)) {
            return "STUB: Logica das perguntas frequentes.";
        }

        if ("3".equals(messageText)) {
            return "STUB: atendimento humano";
        }

        if ("4".equals(messageText)) {
            return "STUB: Gerenciar funcionario(função disponivel apenas para menager)";
        }

        return "Opção" + messageText + "' inválida.";
    }
}
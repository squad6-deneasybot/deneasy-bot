package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.UserProfile;
import com.squad6.deneasybot.repository.UserRepository;
import com.squad6.deneasybot.service.ChatStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);
    private final ReportService reportService;
    private final WhatsAppFormatterService whatsAppFormatterService;
    private final UserRepository userRepository;
    private final ChatStateService chatStateService;

    public MenuService(ReportService reportService,
                       WhatsAppFormatterService whatsAppFormatterService,
                       UserRepository userRepository, ChatStateService chatStateService) {
        this.reportService = reportService;
        this.whatsAppFormatterService = whatsAppFormatterService;
        this.userRepository = userRepository;
        this.chatStateService = chatStateService;
    }

    public String processMenuOption(String userPhone, String messageText) throws IllegalArgumentException {
        logger.info("MenuService: Processando '{}' para {}", messageText, userPhone);


        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> {
                    logger.error("Usuário autenticado {} não encontrado no banco.", userPhone);

                    return new RuntimeException("Usuário autenticado não encontrado.");
                });
        Long companyId = user.getCompany().getId();
        UserProfile profile = user.getProfile();


        switch (messageText.trim()) {
            case "1":
                ReportSimpleDTO report = reportService.getSimpleReport(companyId);
                return whatsAppFormatterService.formatSimpleReport(report);

            case "2":
                return "STUB: Lógica das perguntas frequentes.";

            case "3":
                chatStateService.clearAll(userPhone);
                return "[Finalizando]Para prosseguir com o *atendimento humano*, por favor, entre em contato com o número: \n\n" +
                        "*+55 79 99999-9999*\n\n" +
                        "Agradecemos seu contato. Obrigado por usar o DeneasyBot!👋";

            case "4":
                if (profile == UserProfile.MANAGER) {

                    return "STUB: Gerenciar funcionário (função disponível apenas para manager)";
                } else {

                    throw new IllegalArgumentException("Opção '4' inválida para o perfil EMPLOYEE.");
                }

            default:
                throw new IllegalArgumentException("Opção '" + messageText + "' inválida.");
        }
    }
}
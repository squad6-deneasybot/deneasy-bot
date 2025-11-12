package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.UserProfile;
import com.squad6.deneasybot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);
    private final ReportService reportService;
    private final WhatsAppFormatterService whatsAppFormatterService;
    private final UserRepository userRepository;
    private final ChatStateService chatStateService;
    private final FaqService faqService;

    public MenuService(ReportService reportService,
                       WhatsAppFormatterService whatsAppFormatterService,
                       UserRepository userRepository, ChatStateService chatStateService,
                       FaqService faqService) {
        this.reportService = reportService;
        this.whatsAppFormatterService = whatsAppFormatterService;
        this.userRepository = userRepository;
        this.chatStateService = chatStateService;
        this.faqService = faqService;
    }

    @Transactional(readOnly = true)
    public String processMenuOption(String userPhone, String messageText) throws IllegalArgumentException {
        logger.info("MenuService: Processando '{}' para {}", messageText, userPhone);


        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> {
                    logger.error("Usuário autenticado {} não encontrado no banco.", userPhone);

                    return new RuntimeException("Usuário autenticado não encontrado.");
                });

        UserProfile profile = user.getProfile();

        switch (messageText.trim()) {
            case "1":
                String appKey = user.getCompany().getAppKey();
                String appSecret = user.getCompany().getAppSecret();
                String period = "monthly";

                ReportSimpleDTO report = reportService.generateSimpleReport(appKey, appSecret, period);
                return whatsAppFormatterService.formatSimpleReport(report);

            case "2":
                return faqService.getProjecaoDeCaixa(userPhone);

            case "3":
                chatStateService.clearAll(userPhone);
                return "[Finalizando]Para prosseguir com o *atendimento humano*, por favor, entre em contato com o número: \n\n" +
                        "*+55 79 99999-9999*\n\n" +
                        "Agradecemos seu contato. Obrigado por usar o DeneasyBot!👋";

            case "4":
                if (profile == UserProfile.MANAGER) {
                    return whatsAppFormatterService.formatCrudMenu();
                } else {

                    throw new IllegalArgumentException("Opção '4' inválida para o perfil EMPLOYEE.");
                }

            case "5":
                return whatsAppFormatterService.formatWishlistPrompt();

            default:
                throw new IllegalArgumentException("Opção '" + messageText + "' inválida.");
        }
    }
}
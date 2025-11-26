package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.Frequency;
import com.squad6.deneasybot.model.ReportSimpleDTO;
import com.squad6.deneasybot.model.ReportSubscription;
import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.repository.ReportSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReportScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReportScheduler.class);

    private final ReportSubscriptionRepository subscriptionRepository;
    private final ReportService reportService;
    private final EmailService emailService;
    private final WhatsAppFormatterService formatterService;
    private final EncryptionService encryptionService;

    public ReportScheduler(ReportSubscriptionRepository subscriptionRepository,
                           ReportService reportService,
                           EmailService emailService,
                           WhatsAppFormatterService formatterService,
                           EncryptionService encryptionService) {
        this.subscriptionRepository = subscriptionRepository;
        this.reportService = reportService;
        this.emailService = emailService;
        this.formatterService = formatterService;
        this.encryptionService = encryptionService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processSubscriptions() {
        logger.info("Iniciando Job de envio de relatórios automáticos...");

        List<ReportSubscription> subscriptions = subscriptionRepository.findAll();
        LocalDate today = LocalDate.now();

        int emailsSent = 0;
        int errors = 0;

        for (ReportSubscription sub : subscriptions) {
            try {
                if (shouldSend(sub, today)) {
                    sendReportForSubscription(sub, today);
                    emailsSent++;
                }
            } catch (Exception e) {
                logger.error("Erro ao processar assinatura ID {}: {}", sub.getId(), e.getMessage());
                errors++;
            }
        }

        logger.info("Job de relatórios finalizado. Enviados: {}, Erros: {}", emailsSent, errors);
    }

    private boolean shouldSend(ReportSubscription sub, LocalDate today) {
        if (sub.getLastSentAt() == null) {
            return true;
        }

        LocalDate nextSendDate = switch (sub.getFrequency()) {
            case WEEKLY -> sub.getLastSentAt().plusWeeks(1);
            case BIWEEKLY -> sub.getLastSentAt().plusDays(15);
            case MONTHLY -> sub.getLastSentAt().plusMonths(1);
        };

        return !today.isBefore(nextSendDate);
    }

    private void sendReportForSubscription(ReportSubscription sub, LocalDate today) {
        User user = sub.getUser();

        if (user == null || user.getCompany() == null) {
            logger.warn("Assinatura ID {} ignorada: Usuário ou Empresa inválidos.", sub.getId());
            return;
        }

        LocalDate startDate = calculateStartDate(sub.getFrequency(), today);

        String appKey = encryptionService.decrypt(user.getCompany().getAppKey());
        String appSecret = encryptionService.decrypt(user.getCompany().getAppSecret());

        logger.info("Gerando relatório automático para usuário: {} (Empresa: {})", user.getEmail(), user.getCompany().getName());

        ReportSimpleDTO report = reportService.generateSimpleReport(
                user.getCompany().getName(),
                appKey,
                appSecret,
                startDate,
                today
        );

        String textReport = formatterService.formatSimpleReport(report);
        String htmlReportBody = textReport.replace("\n", "<br>");

        emailService.sendReport(user.getEmail(), user.getName(), htmlReportBody);

        sub.setLastSentAt(today);
        subscriptionRepository.save(sub);
    }

    private LocalDate calculateStartDate(Frequency freq, LocalDate endDate) {
        return switch (freq) {
            case WEEKLY -> endDate.minusWeeks(1);
            case BIWEEKLY -> endDate.minusDays(15);
            case MONTHLY -> endDate.minusMonths(1);
        };
    }
}
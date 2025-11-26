package com.squad6.deneasybot.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendCode(String toEmail, String userName, String code) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            logger.error("O e-mail de destino está vazio ou nulo.");
            throw new IllegalArgumentException("O e-mail de destino não pode ser vazio ou nulo.");
        }
        try {
            InternetAddress emailAddr = new InternetAddress(toEmail);
            emailAddr.validate();
        } catch (AddressException ex) {
            logger.error("Formato de e-mail inválido: {}", toEmail);
            throw new IllegalArgumentException("O e-mail de destino possui formato inválido.", ex);
        }
        if (userName == null || userName.trim().isEmpty()) {
            logger.error("O nome de usuário está vazio ou nulo.");
            throw new IllegalArgumentException("O nome de usuário não pode ser vazio ou nulo.");
        }
        if (code == null || code.trim().isEmpty()) {
            logger.error("O código está vazio ou nulo.");
            throw new IllegalArgumentException("O código não pode ser vazio ou nulo.");
        }

        logger.info("Preparando para enviar código para {}", toEmail);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("DeneasyBot - Seu Código de Verificação");

            String htmlContent = buildHtmlEmailTemplate(userName, code);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            logger.info("E-mail enviado para {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Falha ao enviar e-mail para {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Não foi possível enviar o e-mail de verificação.", e);
        }
    }

    public void sendReport(String toEmail, String userName, String reportHtmlBody) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            logger.error("Tentativa de enviar relatório para e-mail vazio ou nulo.");
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("DeneasyBot - Seu Relatório Automático Chegou 📊");

            String safeUserName = HtmlUtils.htmlEscape(userName != null ? userName : "Cliente");

            String htmlContent = """
                <div style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>
                  <h2 style='color: #0056b3;'>Olá, %s!</h2>
                  <p>Aqui está o seu relatório financeiro automático:</p>
                  <div style='background-color: #f9f9f9; padding: 15px; border: 1px solid #ddd; border-radius: 8px; margin: 20px 0;'>
                    %s
                  </div>
                  <hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'/>
                  <p style='font-size: 0.9em; color: #666;'>
                    Para alterar a frequência de recebimento, acesse o menu do <strong>DeneasyBot</strong> no WhatsApp.
                  </p>
                </div>
                """.formatted(safeUserName, reportHtmlBody);

            helper.setText(htmlContent, true);
            javaMailSender.send(message);
            logger.info("Relatório financeiro enviado com sucesso para {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Erro ao enviar relatório por e-mail para {}", toEmail, e);
        }
    }

    private String buildHtmlEmailTemplate(String userName, String code) {
        String safeUserName = HtmlUtils.htmlEscape(userName);
        String safeCode =  HtmlUtils.htmlEscape(code);

        return "<div style='font-family: Arial, sans-serif; line-height: 1.6;'>"
                + "  <h2>Olá, " + safeUserName + "!</h2>"
                + "  <p>Use o código abaixo para verificar seu e-mail:</p>"
                + "  <div style='background-color: #f4f4f4; padding: 10px 20px; border-radius: 5px; text-align: center;'>"
                + "    <h1 style='color: #333; letter-spacing: 3px; margin: 10px 0;'>" + safeCode + "</h1>"
                + "  </div>"
                + "  <p>Este código é válido por 15 minutos.</p>"
                + "  <p>Equipe DeneasyBot</p>"
                + "</div>";
    }
}
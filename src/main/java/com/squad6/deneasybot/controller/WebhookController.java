package com.squad6.deneasybot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squad6.deneasybot.model.WhatsappMessageRequest;
import com.squad6.deneasybot.service.WebhookOrchestratorService;
import com.squad6.deneasybot.service.WhatsAppSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookOrchestratorService orchestratorService;
    private final WhatsAppSecurityService securityService;
    private final ObjectMapper objectMapper;

    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    public WebhookController(WebhookOrchestratorService orchestratorService,
                             WhatsAppSecurityService securityService,
                             ObjectMapper objectMapper) {
        this.orchestratorService = orchestratorService;
        this.securityService = securityService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token
    ) {
        logger.info("Webhook GET de verificação recebido");

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            logger.info("Verificação do Webhook bem sucedida.");
            return ResponseEntity.ok(challenge);
        } else {
            logger.warn("Falha na verificação do Webhook. Tokens não batem ou modo inválido");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping
    public ResponseEntity<Void> handleMessage(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature
    ) {
        try {
            securityService.validateSignature(rawPayload, signature);
        } catch (SecurityException e) {
            logger.error("Rejeitando requisição não autenticada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            WhatsappMessageRequest request = objectMapper.readValue(rawPayload, WhatsappMessageRequest.class);

            if (request.entry() != null && !request.entry().isEmpty() &&
                    request.entry().getFirst().changes() != null && !request.entry().getFirst().changes().isEmpty() &&
                    request.entry().getFirst().changes().getFirst().value() != null &&
                    request.entry().getFirst().changes().getFirst().value().messages() != null &&
                    !request.entry().getFirst().changes().getFirst().value().messages().isEmpty()) {

                WhatsappMessageRequest.Message message = request.entry().getFirst().changes().getFirst().value().messages().getFirst();

                if (message.text() != null) {
                    String userPhone = message.from();
                    String messageText = message.text().body();

                    logger.info("Webhook POST validado e recebido de {}. Processando...", userPhone);
                    orchestratorService.processMessage(userPhone, messageText);
                } else {
                    logger.info("Mensagem recebida de {}, mas não é texto. Ignorando.", message.from());
                }
            } else {
                logger.debug("Payload do webhook recebido (status update ou estrutura vazia). Ignorando.");
            }

        } catch (JsonProcessingException e) {
            logger.error("Erro ao converter payload do WhatsApp", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Erro inesperado no processamento do webhook", e);
        }

        return ResponseEntity.ok().build();
    }
}
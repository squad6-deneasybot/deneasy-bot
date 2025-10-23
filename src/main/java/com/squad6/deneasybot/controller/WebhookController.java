package com.squad6.deneasybot.controller;

import com.squad6.deneasybot.model.WhatsappMessageRequest;
import com.squad6.deneasybot.service.WebhookOrchestratorService;
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

    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    public WebhookController(WebhookOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token
    ) {
       logger.info("Webhook GET de verificação recebido");
       logger.debug("Mode: {}, Token: {}, Challenge: {}", mode, token, challenge);

       if ("subscribe".equals(mode) && verifyToken.equals(token)) {
           logger.info("Verificação do Webhook bem sucedida. Retornando Challenge.");
           return ResponseEntity.ok(challenge);
       } else {
           logger.warn("Falha na verificação do Webhook. Tokens não batem ou modo invalido");
           return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
       }
    }

    @PostMapping
    public ResponseEntity<Void> handleMessage(@RequestBody WhatsappMessageRequest request) {
        try {
            WhatsappMessageRequest.Message message = request.entry().getFirst().changes().getFirst().value().messages().getFirst();

            if (message.text() != null) {
                String userPhone = message.from();
                String messageText = message.text().body();

                logger.info("Webhook POST recebido de {}. Processando...", userPhone);
                orchestratorService.processMessage(userPhone, messageText);

            } else {
                logger.info("Webhook recebido de {}, mas não é uma mensagem de texto (ex: imagem, áudio). Ignorando.", message.from());
            }

        } catch (Exception e) {
            logger.warn("Payload do webhook recebido não é uma mensagem de usuário padrão (ex: status). Ignorando.");
        }
        return ResponseEntity.ok().build();
    }
}

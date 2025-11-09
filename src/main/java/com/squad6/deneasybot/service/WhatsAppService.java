package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.WhatsAppSendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    private final RestTemplate restTemplate;

    @Value("${whatsapp.api.url}")
    private String apiUrl;

    @Value("${whatsapp.api.token}")
    private String apiToken;

    public WhatsAppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendMessage(String to, String message) {
        logger.info("Enviando mensagem para {}: '{}'", to, message.replace("\n", " "));

        WhatsAppSendRequest payload = new WhatsAppSendRequest(to, message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiToken);

        HttpEntity<WhatsAppSendRequest> entity = new HttpEntity<>(payload, headers);

        try {
            String response = restTemplate.postForObject(apiUrl, entity, String.class);
            logger.info("Mensagem enviada com sucesso para {}", to);
            logger.debug("Resposta da API WhatsApp: {}", response);
        } catch (RestClientException e) {
            logger.error("Falha ao enviar mensagem para {}. Erro: {}", to, e.getMessage());
        }
    }
}

package com.squad6.deneasybot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class WhatsAppSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppSecurityService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${whatsapp.app-secret}")
    private String appSecret;

    public void validateSignature(String payload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            throw new SecurityException("Cabeçalho de assinatura ausente ou inválido.");
        }

        String signatureHash = signatureHeader.substring(7);

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);

            byte[] computedHashBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHash = HexFormat.of().formatHex(computedHashBytes);

            if (!MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8), signatureHash.getBytes(StandardCharsets.UTF_8))) {
                logger.warn("Assinatura inválida! Recebido: {}, Calculado: {}", signatureHash, computedHash);
                throw new SecurityException("Assinatura da requisição inválida.");
            }

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Erro técnico ao validar assinatura", e);
            throw new SecurityException("Erro interno na validação de segurança.");
        }
    }
}
package com.squad6.deneasybot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppSendRequest(
        @JsonProperty("messaging_product") String messagingProduct,
        String to,
        String type,
        Text text
) {

    public WhatsAppSendRequest(String to, String body) {
        this(
                "whatsapp",
                to,
                "text",
                new Text(body)
        );
    }

    public record Text(String body) {}
}
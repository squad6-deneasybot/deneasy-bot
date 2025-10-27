package com.squad6.deneasybot.model;

import java.util.List;

public record WhatsappMessageRequest(List<Entry> entry) {
    public record Entry(
            List<Change> changes
    ) {}

    public record Change(
            Value value
    ) {}

    public record Value(
            List<Message> messages
    ) {}

    public record Message(
            String from,
            Text text
    ) {}

    public record Text(
            String body
    ) {}

}

package com.squad6.deneasybot.controller;

import com.squad6.deneasybot.service.ExampleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final ExampleService exampleService;

    public WebhookController(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody String incomingMessage) {
        String result = exampleService.processExample(incomingMessage);
        return ResponseEntity.ok("Resposta do bot: " + result);
    }
}

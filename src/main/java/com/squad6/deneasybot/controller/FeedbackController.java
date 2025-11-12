package com.squad6.deneasybot.controller;

import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.WishlistDTO;
import com.squad6.deneasybot.service.AuthService;
import com.squad6.deneasybot.service.FeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final AuthService authService;

    public FeedbackController(FeedbackService feedbackService, AuthService authService) {
        this.feedbackService = feedbackService;
        this.authService = authService;
    }
    @PostMapping("/wishlist")
    public ResponseEntity<Void> createWishlist(@RequestBody Map<String, String> payload,
                                               @RequestHeader("Authorization") String authHeader) {
        User user = authService.findUserByToken(extractToken(authHeader));
        String content = payload.get("content");
        feedbackService.saveWishlist(user, content);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @GetMapping("/wishlist")
    public ResponseEntity<List<WishlistDTO>> getWishlist() {

        return ResponseEntity.ok(feedbackService.getAllWishlistItems());
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.squad6.deneasybot.exception.InvalidCredentialsException(
                    "Cabeçalho de autorização ausente ou mal formatado.");
        }
        return authHeader.substring(7);
    }
}
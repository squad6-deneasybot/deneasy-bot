package com.squad6.deneasybot.controller;

import com.squad6.deneasybot.model.VerifyEmailCodeRequestDTO;
import com.squad6.deneasybot.model.VerifyEmailCodeResponseDTO;
import com.squad6.deneasybot.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "").trim();
        authService.logout(token);
        return ResponseEntity.ok("Logout realizado com sucesso.");
    }

    @PostMapping("/verify-email-code")
    public ResponseEntity<VerifyEmailCodeResponseDTO> verifyEmailCode(
            @RequestHeader("X-Code") String inputCode,
            @RequestBody VerifyEmailCodeRequestDTO dto
    ) {
        VerifyEmailCodeResponseDTO response = authService.verifyEmailCode(dto.tokenHash(), inputCode, dto);
        return ResponseEntity.ok(response);
    }
}

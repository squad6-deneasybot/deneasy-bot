package com.squad6.deneasybot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.squad6.deneasybot.model.VerifyEmailCodeRequestDTO;
import com.squad6.deneasybot.model.VerifyEmailCodeResponseDTO;
import com.squad6.deneasybot.model.VerifyEmailRequestDTO;
import com.squad6.deneasybot.model.VerifyEmailResponseDTO;
import com.squad6.deneasybot.service.AuthService;

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
    public ResponseEntity<VerifyEmailCodeResponseDTO> verifyEmailCode(@RequestHeader("X-Code") String inputCode,
            @RequestBody VerifyEmailCodeRequestDTO dto) {
        VerifyEmailCodeResponseDTO response = authService.verifyEmailCode(dto.tokenHash(), inputCode, dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-email")
    public ResponseEntity<VerifyEmailResponseDTO> validateErpUser(@RequestBody VerifyEmailRequestDTO requestDTO) {
        VerifyEmailResponseDTO response = authService.validateUserInErp(requestDTO);
        return ResponseEntity.ok(response);
    }
}

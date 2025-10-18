package com.squad6.deneasybot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.squad6.deneasybot.model.*;
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

    @PostMapping("/request-email-code")
    public ResponseEntity<SendEmailCodeResponseDTO> requestEmailCode(@RequestBody SendEmailCodeRequestDTO request) {
        SendEmailCodeResponseDTO response = authService.requestEmailCode(request);
        return ResponseEntity.ok(response);
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

    @PostMapping("/validate-company")
    public ResponseEntity<CompanyDTO> validateCompany(@RequestBody OmieDTO.VerifyCompanyRequestDTO request) {
        CompanyDTO validatedCompany = authService.validateCompany(request.appKey(), request.appSecret());
        return ResponseEntity.ok(validatedCompany);
    }
}

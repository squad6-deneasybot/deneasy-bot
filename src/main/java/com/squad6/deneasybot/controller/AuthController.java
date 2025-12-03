package com.squad6.deneasybot.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

    @PostMapping("/admin/login")
    public ResponseEntity<AdminAuthResponseDTO> adminLogin(@RequestBody AdminLoginRequestDTO request) {
        AuthService.AdminLoginResult result = authService.loginAdmin(request.email(), request.password());

        ResponseCookie cookie = ResponseCookie.from("accessToken", result.jwt())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();

        AdminAuthResponseDTO responseDTO = new AdminAuthResponseDTO(
                result.admin().getId(),
                result.admin().getName(),
                result.admin().getEmail()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(responseDTO);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logout realizado com sucesso.");
    }

    @PostMapping("/validate-phone")
    public ResponseEntity<ValidatePhoneResponseDTO> validatePhone(@RequestBody ValidatePhoneRequestDTO request) {
        ValidatePhoneResponseDTO response = authService.validatePhone(request);
        return ResponseEntity.ok(response);
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

package com.squad6.deneasybot.service;

import com.squad6.deneasybot.util.CodeGeneratorUtil;
import org.springframework.stereotype.Service;

import com.squad6.deneasybot.client.OmieErpClient;
import com.squad6.deneasybot.exception.InvalidCredentialsException;
import com.squad6.deneasybot.exception.InvalidKeysInErpException;
import com.squad6.deneasybot.exception.UserNotFoundInErpException;
import com.squad6.deneasybot.model.*;
import com.squad6.deneasybot.repository.UserRepository;
import com.squad6.deneasybot.util.JwtUtil;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final OmieErpClient omieErpClient;
    private final EmailService emailService;
    private final CodeGeneratorUtil codeGeneratorUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, OmieErpClient omieErpClient, EmailService emailService, CodeGeneratorUtil codeGeneratorUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.omieErpClient = omieErpClient;
        this.emailService = emailService;
        this.codeGeneratorUtil = codeGeneratorUtil;
    }

    public void logout(String token) {
        User user = userRepository.findBySessionToken(token)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        user.setSessionToken(null);
        userRepository.save(user);
    }

    public SendEmailCodeResponseDTO requestEmailCode(SendEmailCodeRequestDTO request) {
        String code = codeGeneratorUtil.generateRandom6DigitCode();

        String tokenHash = jwtUtil.generateVerificationToken(request.user().getEmail(), code);

        emailService.sendCode(request.user().getEmail(), request.user().getName(), code);

        return new SendEmailCodeResponseDTO(tokenHash);
    }

    public VerifyEmailCodeResponseDTO verifyEmailCode(String hash, String inputCode, VerifyEmailCodeRequestDTO dto) {
        if (!jwtUtil.isTokenValid(hash)) {
            throw new InvalidCredentialsException("Token expirado ou inválido.");
        }

        String storedCode = jwtUtil.extractVerificationCode(hash);
        if (!storedCode.equals(inputCode)) {
            throw new InvalidCredentialsException("Código de verificação incorreto.");
        }

        String sessionToken = jwtUtil.generateSessionToken();

        UserDTO userDTO = dto.user();
        Context context = dto.context();

        switch (context) {
        case REGISTRATION -> {
            userDTO.setSessionToken(sessionToken);
            return new VerifyEmailCodeResponseDTO(userDTO, sessionToken);
        }

        case LOGIN -> {
            User user = userRepository.findByEmail(userDTO.getEmail())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

            user.setSessionToken(sessionToken);
            userRepository.save(user);

            userDTO.setSessionToken(sessionToken);
            return new VerifyEmailCodeResponseDTO(userDTO, sessionToken);
        }

        default -> throw new RuntimeException("Contexto inválido.");
        }
    }

    public VerifyEmailResponseDTO validateUserInErp(VerifyEmailRequestDTO requestDTO) {
        OmieDTO.OmieUserDTO erpUser = omieErpClient
                .findUserByEmail(requestDTO.appKey(), requestDTO.appSecret(), requestDTO.email())
                .orElseThrow(() -> new UserNotFoundInErpException(
                        "Usuário com o e-mail '" + requestDTO.email() + "' não foi encontrado no ERP."));

        UserDTO userDTO = new UserDTO();
        userDTO.setName(erpUser.nome());
        userDTO.setEmail(erpUser.email());
        userDTO.setPhone(erpUser.celular());

        return new VerifyEmailResponseDTO(userDTO, Context.REGISTRATION);
    }

    public CompanyDTO validateCompany(String appKey, String appSecret) {
        return omieErpClient.findCompanyByKeys(appKey, appSecret)
                .orElseThrow(() -> new InvalidKeysInErpException("As credenciais (appKey/appSecret) são inválidas ou não foram encontradas no ERP."));
    }
}

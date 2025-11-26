package com.squad6.deneasybot.service;

import com.squad6.deneasybot.exception.UserNotFoundByPhoneException;
import com.squad6.deneasybot.util.CodeGeneratorUtil;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.squad6.deneasybot.client.OmieErpClient;
import com.squad6.deneasybot.model.SuperAdmin;
import com.squad6.deneasybot.repository.SuperAdminRepository;
import com.squad6.deneasybot.exception.InvalidCredentialsException;
import com.squad6.deneasybot.exception.InvalidKeysInErpException;
import com.squad6.deneasybot.exception.UserNotFoundInErpException;
import com.squad6.deneasybot.model.*;
import com.squad6.deneasybot.repository.UserRepository;
import com.squad6.deneasybot.util.JwtUtil;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final OmieErpClient omieErpClient;
    private final EmailService emailService;
    private final CodeGeneratorUtil codeGeneratorUtil;
    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, OmieErpClient omieErpClient, EmailService emailService, CodeGeneratorUtil codeGeneratorUtil,
                       SuperAdminRepository superAdminRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.omieErpClient = omieErpClient;
        this.emailService = emailService;
        this.codeGeneratorUtil = codeGeneratorUtil;
        this.superAdminRepository = superAdminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminAuthResponseDTO loginAdmin(String email, String password) {
        SuperAdmin admin = superAdminRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new InvalidCredentialsException("E-mail ou senha inválidos.");
        }

        String jwt = jwtUtil.generateSessionToken(admin.getEmail());
        return new AdminAuthResponseDTO(admin.getId(), admin.getName(), admin.getEmail(), jwt);
    }

    public void logout(String token) {
        User user = userRepository.findBySessionToken(token)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        user.setSessionToken(null);
        userRepository.save(user);
    }

    public ValidatePhoneResponseDTO validatePhone(ValidatePhoneRequestDTO request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new UserNotFoundByPhoneException(
                        "Usuário com o telefone '" + request.phone() + "' não foi encontrado."
                ));

        return new ValidatePhoneResponseDTO(new UserDTO(user));
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

        String rawPhone = erpUser.celular();
        String normalizedPhone = normalizePhoneNumber(rawPhone);

        userDTO.setPhone(normalizedPhone);

        return new VerifyEmailResponseDTO(userDTO, Context.REGISTRATION);
    }

    public CompanyDTO validateCompany(String appKey, String appSecret) {
        return omieErpClient.findCompanyByKeys(appKey, appSecret)
                .orElseThrow(() -> new InvalidKeysInErpException("As credenciais (appKey/appSecret) são inválidas ou não foram encontradas no ERP."));
    }

    @Transactional(readOnly = true)
    public User findUserByToken(String sessionToken) {
        if (sessionToken == null || !jwtUtil.isTokenValid(sessionToken)) {
            throw new InvalidCredentialsException("Token inválido ou expirado.");
        }
        return userRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new InvalidCredentialsException("Token de sessão não associado a nenhum usuário."));
    }

    private String normalizePhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }

        String digitsOnly = rawPhone.replaceAll("[^\\d]", "");

        if (digitsOnly.length() == 10) {
            digitsOnly = "55" + digitsOnly;
        } else if (digitsOnly.length() == 11) {
            digitsOnly = "55" + digitsOnly;
        }

        if (digitsOnly.startsWith("55") && digitsOnly.length() == 13 && digitsOnly.charAt(4) == '9') {
            String countryCode = digitsOnly.substring(0, 2);
            String ddd = digitsOnly.substring(2, 4);
            String mainNumber = digitsOnly.substring(5);

            return countryCode + ddd + mainNumber;
        }

        return digitsOnly;
    }
}

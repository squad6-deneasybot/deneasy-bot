package com.squad6.deneasybot.service;

import com.squad6.deneasybot.exception.InvalidCredentialsException;
import com.squad6.deneasybot.model.*;
import com.squad6.deneasybot.repository.UserRepository;
import com.squad6.deneasybot.util.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public void logout(String token) {
        User user = userRepository.findBySessionToken(token)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        user.setSessionToken(null);
        userRepository.save(user);
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

        UserDTO userDTO = dto.getUser();
        Context context = dto.getContext();

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
}

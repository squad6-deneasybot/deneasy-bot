package com.squad6.deneasybot.service;

import org.springframework.stereotype.Service;

import com.squad6.deneasybot.client.OmieErpClient;
import com.squad6.deneasybot.exception.InvalidCredentialsException;
import com.squad6.deneasybot.exception.UserNotFoundInErpException;
import com.squad6.deneasybot.model.Context;
import com.squad6.deneasybot.model.OmieUserDTO;
import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.UserDTO;
import com.squad6.deneasybot.model.VerifyEmailCodeRequestDTO;
import com.squad6.deneasybot.model.VerifyEmailCodeResponseDTO;
import com.squad6.deneasybot.model.VerifyEmailRequestDTO;
import com.squad6.deneasybot.model.VerifyEmailResponseDTO;
import com.squad6.deneasybot.repository.UserRepository;
import com.squad6.deneasybot.util.JwtUtil;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final OmieErpClient omieErpClient;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, OmieErpClient omieErpClient) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.omieErpClient = omieErpClient;
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
        OmieUserDTO erpUser = omieErpClient
                .findUserByEmail(requestDTO.appKey(), requestDTO.appSecret(), requestDTO.email())
                .orElseThrow(() -> new UserNotFoundInErpException(
                        "Usuário com o e-mail '" + requestDTO.email() + "' não foi encontrado no ERP."));

        UserDTO userDTO = new UserDTO();
        userDTO.setName(erpUser.nome());
        userDTO.setEmail(erpUser.email());

        return new VerifyEmailResponseDTO(userDTO, Context.REGISTRATION);
    }
}

package com.squad6.deneasybot.service;

import com.squad6.deneasybot.exception.InvalidCredentialsException;
import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.UserDTO;
import com.squad6.deneasybot.repository.UserRepository;
import com.squad6.deneasybot.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * STUB (Mock) para RF-USER-03 (Dupla 1).
     * Simula a criação de um usuário no banco.
     */
    public User createUser(UserDTO dto) {
        logger.warn("STUB (UserService): Método 'createUser' chamado para {}. Simulando criação.", dto.getEmail());

        // O Orchestrator não usa o retorno, mas é bom retornar um mock.
        User mockUser = new User();
        mockUser.setId(1L); // ID Fixo de Mock
        mockUser.setName(dto.getName());
        mockUser.setEmail(dto.getEmail());
        mockUser.setProfile(dto.getProfile());
        return mockUser;
    }

    public UserDTO getMyProfile(String sessionToken) {
        if (sessionToken == null || !jwtUtil.isTokenValid(sessionToken)) {
            throw new InvalidCredentialsException("Token de sessão inválido ou expirado.");
        }

        User user = userRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new InvalidCredentialsException("Usuário não encontrado para o token fornecido."));
        return new UserDTO(user);
    }
}
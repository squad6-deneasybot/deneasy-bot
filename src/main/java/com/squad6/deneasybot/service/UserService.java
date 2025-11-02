package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.UserDTO;
import com.squad6.deneasybot.model.UserMeResponseDTO;
import com.squad6.deneasybot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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


    @Autowired
    private UserRepository userRepository;

    public UserMeResponseDTO getAuthenticatedUser(Authentication authentication) {

        String userEmail = getEmailFromAuthentication(authentication);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com email: " + userEmail));

        return UserMeResponseDTO.fromEntity(user);
    }

    private String getEmailFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {

            throw new SecurityException("Acesso negado. Usuário não encontrado");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }
}
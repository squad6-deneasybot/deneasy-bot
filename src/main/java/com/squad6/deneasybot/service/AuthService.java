package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void logout(String token) {
        User user = userRepository.findBySessionToken(token)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        user.setSessionToken(null);
        userRepository.save(user);
    }
}

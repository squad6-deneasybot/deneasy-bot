package com.squad6.deneasybot.repository;

import com.squad6.deneasybot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findBySessionToken(String token);
}

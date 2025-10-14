package com.squad6.deneasybot.repository;

import com.squad6.deneasybot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

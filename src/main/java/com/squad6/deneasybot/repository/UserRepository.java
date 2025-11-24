package com.squad6.deneasybot.repository;

import com.squad6.deneasybot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findBySessionToken(String token);

    Optional<User> findByPhone(String phone);

    @Query("SELECT u FROM User u JOIN FETCH u.company WHERE u.phone = :phone")
    Optional<User> findByPhoneWithCompany(@Param("phone") String phone);

    List<User> findAllByCompanyId(Long companyId);
}

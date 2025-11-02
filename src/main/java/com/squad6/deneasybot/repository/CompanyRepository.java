package com.squad6.deneasybot.repository;

import com.squad6.deneasybot.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByAppKey(String appKey);
}

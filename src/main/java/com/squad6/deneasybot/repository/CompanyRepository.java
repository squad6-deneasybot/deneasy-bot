package com.squad6.deneasybot.repository;

import com.squad6.deneasybot.model.Company;
import com.squad6.deneasybot.model.CompanyDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByAppKey(String appKey);

    @Query("SELECT new com.squad6.deneasybot.model.CompanyDTO(" +
            "c.id, c.name, c.appKey, c.appSecret, " +
            "u.id, u.name, u.email, u.phone) " +
            "FROM Company c " +
            "LEFT JOIN c.users u ON (u.company.id = c.id AND u.profile = 'MANAGER')")
    List<CompanyDTO> findAllWithManager();
}
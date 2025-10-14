package com.squad6.deneasybot.repository;

import com.squad6.deneasybot.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}

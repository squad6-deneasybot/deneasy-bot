package com.squad6.deneasybot.service;

import com.squad6.deneasybot.exception.DataIntegrityException;
import com.squad6.deneasybot.exception.ResourceNotFoundException;
import com.squad6.deneasybot.model.Company;
import com.squad6.deneasybot.model.CompanyDTO;
import com.squad6.deneasybot.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);
    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional
    public Company createCompany(CompanyDTO dto) {
        logger.info("Criando nova empresa no banco: {}", dto.getCompanyName());

        companyRepository.findByAppKey(dto.getAppKey()).ifPresent(existingCompany -> {
            throw new DataIntegrityException("Já existe uma empresa cadastrada com esta App Key.");
        });

        Company company = new Company();
        company.setName(dto.getCompanyName());
        company.setAppKey(dto.getAppKey());
        company.setAppSecret(dto.getAppSecret());

        return companyRepository.save(company);
    }

    @Transactional
    public CompanyDTO updateCompany(Long id, CompanyDTO companyDetails) {
        logger.info("Atualizando empresa ID {} com novos dados.", companyDetails.getCompanyName());

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa com ID " + id + " não encontrada."));

        companyRepository.findByAppKey(companyDetails.getAppKey())
                .filter(existingCompany -> !existingCompany.getId().equals(id))
                .ifPresent(existingCompany -> {
                    throw new DataIntegrityException("Já existe uma empresa cadastrada com esta App Key.");
                });

        company.setName(companyDetails.getCompanyName());
        company.setAppKey(companyDetails.getAppKey());
        company.setAppSecret(companyDetails.getAppSecret());

        Company updatedCompany = companyRepository.save(company);
        return new CompanyDTO(updatedCompany);
    }

    @Transactional(readOnly = true)
    public List<CompanyDTO> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(CompanyDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompanyDTO getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa com ID " + id + " não encontrada."));
        return new CompanyDTO(company);
    }
}
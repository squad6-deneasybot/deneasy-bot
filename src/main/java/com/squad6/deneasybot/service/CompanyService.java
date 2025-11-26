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

@Service
public class CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);
    private final CompanyRepository companyRepository;
    private final EncryptionService encryptionService;

    public CompanyService(CompanyRepository companyRepository, EncryptionService encryptionService) {
        this.companyRepository = companyRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public Company createCompany(CompanyDTO dto) {
        logger.info("Criando nova empresa no banco: {}", dto.getCompanyName());

        // Nota: A validação findByAppKey pode precisar de ajuste se a chave estiver criptografada,
        // pois a busca exata falhará se o IV for aleatório.
        // Assumindo que a validação é feita antes ou o risco de colisão é gerido de outra forma.

        Company company = new Company();
        company.setName(dto.getCompanyName());

        // Criptografia antes de salvar (Fluxo de Escrita)
        company.setAppKey(encryptionService.encrypt(dto.getAppKey()));
        company.setAppSecret(encryptionService.encrypt(dto.getAppSecret()));

        return companyRepository.save(company);
    }

    @Transactional
    public CompanyDTO updateCompany(Long id, CompanyDTO companyDetails) {
        logger.info("Atualizando empresa ID {} com novos dados.", companyDetails.getCompanyName());

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa com ID " + id + " não encontrada."));

        company.setName(companyDetails.getCompanyName());

        // Criptografia antes de salvar (Fluxo de Escrita)
        company.setAppKey(encryptionService.encrypt(companyDetails.getAppKey()));
        company.setAppSecret(encryptionService.encrypt(companyDetails.getAppSecret()));

        Company updatedCompany = companyRepository.save(company);
        return new CompanyDTO(updatedCompany);
    }

    @Transactional
    public void deleteCompany(Long id) {
        logger.warn("Tentativa de deletar empresa ID {}", id);
        if (!companyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Empresa com ID " + id + " não encontrada.");
        }
        try {
            companyRepository.deleteById(id);
            logger.info("Empresa ID {} deletada com sucesso.", id);
        } catch (Exception e) {
            logger.error("Erro ao deletar empresa ID {}, {}", id, e.getMessage());
            throw new DataIntegrityException("Não foi possível deletar a empresa. Verifique se há registros vinculados.");
        }
    }

    @Transactional(readOnly = true)
    public List<CompanyDTO> getAllCompanies() {
        return companyRepository.findAllWithManager();
    }

    @Transactional(readOnly = true)
    public CompanyDTO getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa com ID " + id + " não encontrada."));
        return new CompanyDTO(company);
    }
}
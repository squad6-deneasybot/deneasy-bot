package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.Company;
import com.squad6.deneasybot.model.CompanyDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    /**
     * STUB (Mock) para RF-AUTH-03.
     */
    public CompanyDTO validateCompanyInErp(String appKey, String appSecret) {
        logger.warn("STUB (CompanyService): Método 'validateCompanyInErp' chamado. Simulando SUCESSO.");

        // Para testar o fluxo de ERRO:
        // if (appKey.equals("erro")) {
        //     logger.warn("STUB: Simulando erro de InvalidKeysInErpException");
        //     throw new InvalidKeysInErpException("Mock: Chaves inválidas");
        // }

        // Agora isto funciona, pois CompanyDTO é uma CLASSE
        CompanyDTO mockDto = new CompanyDTO();
        mockDto.setCompanyName("Mock Company (Stub)");
        mockDto.setAppKey(appKey);
        mockDto.setAppSecret(appSecret);
        return mockDto;
    }

    /**
     * STUB (Mock) para RF-COMPANY-01.
     */
    public Company createCompany(CompanyDTO dto) {
        // E agora isto também funciona
        logger.warn("STUB (CompanyService): Método 'createCompany' chamado para {}. Simulando criação.", dto.getCompanyName());

        Company mockCompany = new Company();
        mockCompany.setId(1L); // ID Fixo de Mock

        mockCompany.setName(dto.getCompanyName());
        return mockCompany;
    }
}
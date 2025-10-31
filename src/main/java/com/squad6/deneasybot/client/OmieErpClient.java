package com.squad6.deneasybot.client;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squad6.deneasybot.model.*;

@Component
public class OmieErpClient {

    private static final Logger logger = LoggerFactory.getLogger(OmieErpClient.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter OMIE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int REGISTROS_POR_PAGINA = 100;

    @Value("${omie.api.users.url}")
    private String omieUsersApiUrl;

    @Value("${omie.api.companies.url}")
    private String omieCompaniesApiUrl;

    @Value("${omie.api.movements.url}")
    private String omieMovementsApiUrl;

    public OmieErpClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<OmieDTO.OmieUserDTO> findUserByEmail(String appKey, String appSecret, String email) {
        var param = new OmieDTO.UserRequestParam(1, email);
        var requestBody = new OmieDTO.UserRequest("ListarUsuarios", appKey, appSecret, List.of(param));

        try {
            OmieDTO.UserResponse response = restTemplate.postForObject(omieUsersApiUrl, requestBody, OmieDTO.UserResponse.class);

            if (response != null && response.totalDeRegistros() > 0 && response.cadastros() != null
                    && !response.cadastros().isEmpty()) {
                return Optional.of(response.cadastros().getFirst());
            }

            return Optional.empty();

        } catch (RestClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();

            try {
                OmieDTO.UsersErrorResponse errorResponse = objectMapper.readValue(errorBody, OmieDTO.UsersErrorResponse.class);

                if (errorResponse != null && errorResponse.faultstring() != null &&
                        errorResponse.faultstring().toLowerCase().contains("vendedor não cadastrado")) {

                    logger.warn("A API da Omie retornou um erro de 'não encontrado' (faultstring) para o e-mail: {}", email);

                    return Optional.empty();
                }
            } catch (JsonProcessingException jsonEx) {
                logger.error("Não foi possível fazer o parse da resposta de erro da Omie. Body: {}", errorBody, jsonEx);
            }

            logger.error("Erro inesperado do cliente ao chamar a API Omie: Status {}, Body {}", e.getStatusCode(), errorBody);
            throw new RuntimeException("Falha na validação com o ERP.", e);

        } catch (RestClientException e) {
            logger.error("Erro de comunicação ao chamar a API Omie: {}", e.getMessage());
            throw new RuntimeException("Não foi possível comunicar com o serviço do ERP.", e);
        }
    }

    public Optional<CompanyDTO> findCompanyByKeys(String appKey, String appSecret) {
        var param = new OmieDTO.CompanyRequestParam(1);
        var requestBody = new OmieDTO.CompanyRequest("ListarEmpresas", appKey, appSecret, List.of(param));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));


        HttpEntity<OmieDTO.CompanyRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<OmieDTO.CompanyResponse> responseEntity = restTemplate.exchange(
                    omieCompaniesApiUrl,
                    HttpMethod.POST,
                    entity,
                    OmieDTO.CompanyResponse.class
            );

            OmieDTO.CompanyResponse response = responseEntity.getBody();

            if (response != null && response.empresasCadastro() != null && !response.empresasCadastro().isEmpty()) {
                OmieDTO.CompanyDetails companyDetails = response.empresasCadastro().getFirst();
                CompanyDTO companyDTO = new CompanyDTO();
                companyDTO.setCompanyName(companyDetails.razaoSocial());
                companyDTO.setAppKey(appKey);
                companyDTO.setAppSecret(appSecret);
                return Optional.of(companyDTO);
            }

            return Optional.empty();

        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                logger.warn("A API da Omie retornou 403 Forbidden, indicando chaves inválidas.");
                return Optional.empty();
            } else {
                logger.error("Erro inesperado ao chamar a API Omie (ListarEmpresas): Status {}, Body {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new RuntimeException("Falha na validação com o ERP.", e);
            }
        } catch (RestClientException e) {
            logger.error("Erro de comunicação ao chamar a API Omie (ListarEmpresas): {}", e.getMessage());
            throw new RuntimeException("Não foi possível comunicar com o serviço do ERP.", e);
        }
    }
    public OmieDTO.MovementResponse listMovements(String appKey, String appSecret, int pageNumber, LocalDate startDate, LocalDate endDate) {

        String dDtDe = startDate.format(OMIE_DATE_FORMATTER);
        String dDtAte = endDate.format(OMIE_DATE_FORMATTER);

        var param = new OmieDTO.MovementParam(pageNumber, REGISTROS_POR_PAGINA, dDtDe, dDtAte);
        var requestBody = new OmieDTO.MovementRequest("ListarMovimentos", appKey, appSecret, List.of(param));

        try {
            return restTemplate.postForObject(omieMovementsApiUrl, requestBody, OmieDTO.MovementResponse.class);

        } catch (RestClientResponseException e) {
            // Tratamento de erro similar aos outros métodos
            String errorBody = e.getResponseBodyAsString();
            logger.error("Erro inesperado do cliente ao chamar a API Omie (ListarMovimentos): Status {}, Body {}", e.getStatusCode(), errorBody, e);
            throw new RuntimeException("Falha na busca de movimentos com o ERP.", e);

        } catch (RestClientException e) {
            logger.error("Erro de comunicação ao chamar a API Omie (ListarMovimentos): {}", e.getMessage(), e);
            throw new RuntimeException("Não foi possível comunicar com o serviço do ERP.", e);
        }
    }
}

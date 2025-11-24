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

    public static final DateTimeFormatter OMIE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final int MAX_REGISTROS_POR_PAGINA = 1000;

    @Value("${omie.api.users.url}")
    private String omieUsersApiUrl;

    @Value("${omie.api.companies.url}")
    private String omieCompaniesApiUrl;

    @Value("${omie.api.movements.url}")
    private String omieMovementsApiUrl;

    @Value("${omie.api.categories.url}")
    private String omieCategoriesApiUrl;

    @Value("${omie.api.summary.url}")
    private String omieSummaryApiUrl;

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

    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "omieApi")
    @io.github.resilience4j.bulkhead.annotation.Bulkhead(name = "omieApi")
    @io.github.resilience4j.retry.annotation.Retry(name = "omieApi")
    public OmieDTO.MovementResponse listMovements(String appKey, String appSecret, OmieDTO.MovementFilterParam param) {

        var requestBody = new OmieDTO.MovementRequest("ListarMovimentos", appKey, appSecret, List.of(param));

        try {
            ResponseEntity<OmieDTO.MovementResponse> responseEntity = restTemplate.postForEntity(
                    omieMovementsApiUrl,
                    requestBody,
                    OmieDTO.MovementResponse.class
            );

            return responseEntity.getBody();

        } catch (RestClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            logger.error("Erro inesperado do cliente (ListarMovimentos): Status {}, Body {}", e.getStatusCode(), errorBody, e);
            throw new RuntimeException("Falha na busca de movimentos com o ERP.", e);

        } catch (RestClientException e) {
            logger.error("Erro de comunicação (ListarMovimentos): {}", e.getMessage(), e);
            throw new RuntimeException("Não foi possível comunicar com o serviço do ERP.", e);
        }
    }

    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "omieApi")
    @io.github.resilience4j.bulkhead.annotation.Bulkhead(name = "omieApi")
    @io.github.resilience4j.retry.annotation.Retry(name = "omieApi")
    public OmieDTO.OmieCategoryDTO consultCategory(String appKey, String appSecret, String categoryCode) {
        var param = new OmieDTO.CategoryRequestParam(categoryCode);
        var requestBody = new OmieDTO.CategoryRequest("ConsultarCategoria", appKey, appSecret, List.of(param));

        try {
            OmieDTO.OmieCategoryDTO response = restTemplate.postForObject(
                    omieCategoriesApiUrl,
                    requestBody,
                    OmieDTO.OmieCategoryDTO.class
            );

            if (response == null) {
                logger.error("Resposta nula da API Omie (ConsultarCategoria) para o código: {}", categoryCode);
                throw new RuntimeException("Falha ao consultar categoria no ERP: resposta nula.");
            }

            return response;

        } catch (RestClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            logger.error("Erro inesperado do cliente (ConsultarCategoria): Status {}, Body {}", e.getStatusCode(), errorBody, e);
            throw new RuntimeException("Falha na consulta de categoria com o ERP.", e);

        } catch (RestClientException e) {
            logger.error("Erro de comunicação (ConsultarCategoria): {}", e.getMessage(), e);
            throw new RuntimeException("Não foi possível comunicar com o serviço do ERP.", e);
        }
    }

    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "omieApi")
    @io.github.resilience4j.bulkhead.annotation.Bulkhead(name = "omieApi")
    @io.github.resilience4j.retry.annotation.Retry(name = "omieApi")
    public OmieDTO.FinancialSummaryResponse getFinancialSummary(String appKey, String appSecret, LocalDate date) {
        String dDia = date.format(OMIE_DATE_FORMATTER);
        var param = new OmieDTO.FinancialSummaryParam(dDia);
        var requestBody = new OmieDTO.FinancialSummaryRequest("ObterResumoFinancas", appKey, appSecret, List.of(param));

        logger.debug("Chamando Omie API (ObterResumoFinancas) para data: {}", dDia);

        try {
            ResponseEntity<OmieDTO.FinancialSummaryResponse> responseEntity = restTemplate.postForEntity(
                    omieSummaryApiUrl,
                    requestBody,
                    OmieDTO.FinancialSummaryResponse.class
            );

            OmieDTO.FinancialSummaryResponse response = responseEntity.getBody();

            if (response == null) {
                logger.error("Resposta nula da API Omie (ObterResumoFinancas) para a data: {}", dDia);
                throw new RuntimeException("Falha ao obter resumo financeiro: resposta nula.");
            }

            return response;

        } catch (RestClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            logger.error("Erro inesperado do cliente (ObterResumoFinancas): Status {}, Body {}", e.getStatusCode(), errorBody);
            throw new RuntimeException("Falha na busca do resumo financeiro com o ERP.", e);

        } catch (RestClientException e) {
            logger.error("Erro de comunicação (ObterResumoFinancas): {}", e.getMessage(), e);
            throw new RuntimeException("Não foi possível comunicar com o serviço do ERP.", e);
        }
    }
}

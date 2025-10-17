package com.squad6.deneasybot.client;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squad6.deneasybot.model.OmieDTO;

@Component
public class OmieErpClient {

    private static final Logger logger = LoggerFactory.getLogger(OmieErpClient.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${omie.api.url-user}")
    private String omieApiUrl;

    public OmieErpClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<OmieDTO.OmieUserDTO> findUserByEmail(String appKey, String appSecret, String email) {
        var param = new OmieDTO.UserRequestParam(1, email);
        var requestBody = new OmieDTO.UserRequest("ListarUsuarios", appKey, appSecret, List.of(param));

        try {
            OmieDTO.UserResponse response = restTemplate.postForObject(omieApiUrl, requestBody, OmieDTO.UserResponse.class);

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
}

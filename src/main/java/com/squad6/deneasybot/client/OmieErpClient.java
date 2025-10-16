package com.squad6.deneasybot.client;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.squad6.deneasybot.model.OmieRequestDTO;
import com.squad6.deneasybot.model.OmieRequestParamDTO;
import com.squad6.deneasybot.model.OmieResponseDTO;
import com.squad6.deneasybot.model.OmieUserDTO;

@Component
public class OmieErpClient {

    private static final Logger logger = LoggerFactory.getLogger(OmieErpClient.class);
    private final RestTemplate restTemplate;

    @Value("${omie.api.url}")
    private String omieApiUrl;

    public OmieErpClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<OmieUserDTO> findUserByEmail(String appKey, String appSecret, String email) {
        var param = new OmieRequestParamDTO(1, email);
        var requestBody = new OmieRequestDTO("ListarUsuarios", appKey, appSecret, List.of(param));

        try {
            OmieResponseDTO response = restTemplate.postForObject(omieApiUrl, requestBody, OmieResponseDTO.class);

            if (response != null && response.totalDeRegistros() > 0 && response.cadastros() != null
                    && !response.cadastros().isEmpty()) {

                return Optional.of(response.cadastros().getFirst());
            }
        } catch (HttpClientErrorException e) {

            logger.error("Erro de cliente ao chamar a API Omie: Status {}, Body {}", e.getStatusCode(),
                    e.getResponseBodyAsString());

            throw new RuntimeException("Falha na validação com o ERP: " + e.getMessage(), e);
        } catch (RestClientException e) {

            logger.error("Erro de comunicação ao chamar a API Omie: {}", e.getMessage());
            throw new RuntimeException("Não foi possível comunicar com o serviço do ERP.", e);
        }

        return Optional.empty();
    }
}
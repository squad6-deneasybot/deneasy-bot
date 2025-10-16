package com.squad6.deneasybot.client;

import com.squad6.deneasybot.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Component
public class OmieErpClient {

    private final RestTemplate restTemplate;

    @Value("${omie.api.url}")
    private String omieApiUrl;

    public OmieErpClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<OmieUserDTO> findUserByEmail(String appKey, String appSecret, String email) {
        var param = new OmieRequestParamDTO(1, email);
        var requestBody = new OmieRequestDTO("ListarUsuarios", appKey, appSecret, List.of(param));

        OmieResponseDTO response = restTemplate.postForObject(omieApiUrl, requestBody, OmieResponseDTO.class);

        if (response != null && response.totalDeRegistros() > 0 && response.cadastros() != null && !response.cadastros().isEmpty()) {
            return Optional.of(response.cadastros().getFirst());
        }

        return Optional.empty();
    }
}

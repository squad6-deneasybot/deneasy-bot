package com.squad6.deneasybot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OmieResponseDTO(
        @JsonProperty("total_de_registros") int totalDeRegistros,
        @JsonProperty("cadastros") List<OmieUserDTO> cadastros
) {}
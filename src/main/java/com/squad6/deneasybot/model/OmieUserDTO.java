package com.squad6.deneasybot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OmieUserDTO(
        @JsonProperty("cNome") String nome,
        @JsonProperty("cEmail") String email,
        @JsonProperty("cCelular") String celular,
        @JsonProperty("cTelefone") String telefone
) {}
package com.squad6.deneasybot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class OmieDTO {

    private OmieDTO() {}

    public record UserRequest(
            String call,
            String app_key,
            String app_secret,
            List<UserRequestParam> param
    ) {}

    public record UserRequestParam(
            int pagina,
            String email_usuario
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserResponse(
            @JsonProperty("total_de_registros") int totalDeRegistros,
            @JsonProperty("cadastros") List<OmieUserDTO> cadastros
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OmieUserDTO(
            @JsonProperty("cNome") String nome,
            @JsonProperty("cEmail") String email,
            @JsonProperty("cCelular") String celular,
            @JsonProperty("cTelefone") String telefone
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsersErrorResponse(
            String faultstring
    ) {}
}
package com.squad6.deneasybot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VerifyCompanyRequestDTO(String appKey, String appSecret) {}

    public record CompanyRequest(
            String call,
            @JsonProperty("app_key") String appKey,
            @JsonProperty("app_secret") String appSecret,
            List<CompanyRequestParam> param) {}

    public record CompanyRequestParam(int pagina) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompanyResponse(@JsonProperty("empresas_cadastro") List<CompanyDetails> empresasCadastro) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompanyDetails(@JsonProperty("razao_social") String razaoSocial) {}

    public record MovementParam(
            @JsonProperty("nPagina") int nPagina,
            @JsonProperty("nRegPorPagina") int nRegPorPagina,
            @JsonProperty("dDtDe") String dDtDe,
            @JsonProperty("dDtAte") String dDtAte
    ) {}

    public record MovementRequest(
            String call,
            @JsonProperty("app_key") String appKey,
            @JsonProperty("app_secret") String appSecret,
            List<MovementParam> param
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovementResponse(
            @JsonProperty("nPagina") int nPagina,
            @JsonProperty("nTotPaginas") int nTotPaginas,
            @JsonProperty("nTotRegistros") int nTotRegistros,
            @JsonProperty("movimento") List<MovimentoDetalhe> movimento
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovimentoDetalhe(
            @JsonProperty("detalhes") Detalhe detalhes,
            @JsonProperty("resumo") Resumo resumo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Detalhe(
            @JsonProperty("dDtInc") String dDtInc, // Data de Inclusão
            @JsonProperty("dDtComp") String dDtComp, // Data de Competência
            @JsonProperty("dDtVenc") String dDtVenc, // Data de Vencimento
            @JsonProperty("cDesc") String cDesc, // Descrição
            @JsonProperty("cCodCateg") String cCodCateg // Código da Categoria
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resumo(
            @JsonProperty("nValor") BigDecimal nValor, // Valor do Movimento
            @JsonProperty("nValPago") BigDecimal nValPago, // Valor Pago
            @JsonProperty("nValJuros") BigDecimal nValJuros, // Valor Juros
            @JsonProperty("nValDesc") BigDecimal nValDesc // Valor Desconto
    ) {}
}
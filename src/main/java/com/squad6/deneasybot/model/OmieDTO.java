package com.squad6.deneasybot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MovementFilterParam(
            @JsonProperty("nPagina") Integer nPagina,
            @JsonProperty("nRegPorPagina") Integer nRegPorPagina,
            @JsonProperty("dDtVencDe") String dDtVencDe,
            @JsonProperty("dDtVencAte") String dDtVencAte
    ) {}

    public record MovementRequest(
            String call,
            @JsonProperty("app_key") String appKey,
            @JsonProperty("app_secret") String appSecret,
            List<MovementFilterParam> param
    ) {}


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovementResponse(
            @JsonProperty("nPagina") int nPagina,
            @JsonProperty("nTotPaginas") int nTotPaginas,
            @JsonProperty("nTotRegistros") int nTotRegistros,
            @JsonProperty("movimentos") List<MovementDetail> movements
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovementDetail(
            @JsonProperty("detalhes") MovementHeader header,
            @JsonProperty("resumo") MovementSummary summary
    ) {}


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovementHeader(
            @JsonProperty("cCodCateg") String cCodCateg,
            @JsonProperty("cStatus") String cStatus,
            @JsonProperty("cGrupo") String cGrupo,
            @JsonProperty("dDtPagamento") String dDtPagamento,
            @JsonProperty("dDtVenc") String dDtVenc,
            @JsonProperty("nValorTitulo") BigDecimal nValorTitulo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovementSummary(
            @JsonProperty("nValPago") BigDecimal nValPago,
            @JsonProperty("nValRecebido") BigDecimal nValRecebido,
            @JsonProperty("nValAberto") BigDecimal nValAberto
    ) {}

    public record CategoryRequest(
            String call,
            @JsonProperty("app_key") String appKey,
            @JsonProperty("app_secret") String appSecret,
            List<CategoryRequestParam> param
    ) {}

    public record CategoryRequestParam(
            @JsonProperty("codigo") String codigo
    ) {}

    public record CategoryListRequest(
            String call,
            @JsonProperty("app_key") String appKey,
            @JsonProperty("app_secret") String appSecret,
            List<CategoryListParam> param
    ) {}

    public record CategoryListParam(
            @JsonProperty("pagina") int pagina,
            @JsonProperty("registros_por_pagina") int registrosPorPagina
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryListResponse(
            @JsonProperty("pagina") int pagina,
            @JsonProperty("total_de_paginas") int totalDePaginas,
            @JsonProperty("registros") int registros,
            @JsonProperty("total_de_registros") int totalDeRegistros,
            @JsonProperty("categoria_cadastro") List<OmieCategoryDTO> categorias
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OmieCategoryDTO(
            @JsonProperty("codigo") String codigo,
            @JsonProperty("descricao") String descricao
    ) {}

    public record FinancialSummaryRequest(
            String call,
            @JsonProperty("app_key") String appKey,
            @JsonProperty("app_secret") String appSecret,
            List<FinancialSummaryParam> param
    ) {}

    public record FinancialSummaryParam(
            @JsonProperty("dDia") String dDia
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinancialSummaryResponse(
            @JsonProperty("contaCorrente") CurrentAccountSummary contaCorrente
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentAccountSummary(
            @JsonProperty("vTotal") BigDecimal vTotal
    ) {}

}

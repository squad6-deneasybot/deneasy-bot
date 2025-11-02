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
            @JsonProperty("movimentos") List<MovementDetail> movements // <-- Corrigido (nome JSON "movimentos")
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovementDetail(
            @JsonProperty("detalhes") MovementHeader header, // Renomeado de "Detalhe"
            @JsonProperty("resumo") MovementSummary summary     // Renomeado de "Resumo"
    ) {}


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovementHeader(
            @JsonProperty("cCodCateg") String cCodCateg, // Código da Categoria
            @JsonProperty("cStatus") String cStatus,     // Ex: "PAGO", "ATRASADO"
            @JsonProperty("cGrupo") String cGrupo,       // Ex: "CONTA_A_PAGAR"
            @JsonProperty("dDtPagamento") String dDtPagamento, // Data de Pagamento (para filtrar)
            @JsonProperty("dDtVenc") String dDtVenc     // Data de Vencimento
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovementSummary(
            @JsonProperty("nValPago") BigDecimal nValPago,     // Valor Pago
            @JsonProperty("nValRecebido") BigDecimal nValRecebido, // Valor Recebido (para receitas)
            @JsonProperty("nValAberto") BigDecimal nValAberto    // Valor em Aberto
    ) {}
}

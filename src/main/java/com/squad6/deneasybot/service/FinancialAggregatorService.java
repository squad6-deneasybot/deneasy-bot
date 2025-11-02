package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ReportSimpleDTO;
import com.squad6.deneasybot.model.OmieDTO.MovementDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class FinancialAggregatorService {

    private static final String STATUS_PAGO = "PAGO";
    private static final String STATUS_RECEBIDO = "RECEBIDO";
    private static final String GRUPO_CONTA_A_PAGAR = "CONTA_A_PAGAR";
    private static final String GRUPO_CONTA_A_RECEBER = "CONTA_A_RECEBER";

    private final MovementFetcherService movementFetcherService;
    private final CategoryCacheService categoryCacheService;

    @Autowired
    public FinancialAggregatorService(MovementFetcherService movementFetcherService, CategoryCacheService categoryCacheService) {
        this.movementFetcherService = movementFetcherService;
        this.categoryCacheService = categoryCacheService;
    }

    /**
     * Calcula os totais do relatório agregando movimentos financeiros.
     *
     * @param appKey A chave da aplicação.
     * @param appSecret O segredo da aplicação.
     * @param period O período para agregação ("weekly" ou "monthly").
     * @return O ReportSimpleDTO completo com os totais calculados.
     */
    public ReportSimpleDTO aggregateReportData(String appKey, String appSecret, String period) {
        // Define startDate e endDate com base no period (Critério: Define startDate e endDate)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period.toLowerCase()) {
            case "weekly":
                startDate = endDate.minusDays(7);
                break;
            case "monthly":
                startDate = endDate.minusDays(30);
                break;
            default:
                throw new IllegalArgumentException("Período inválido: " + period + ". Use 'weekly' ou 'monthly'.");
        }

        // Chama movementFetcher.fetchAllMovementsForPeriod(...) (Critério: Chama movementFetcher...)
        List<MovementDetail> movements = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret, startDate, endDate);

        // Inicializa os acumuladores (Critério: Inicializa os acumuladores)
        BigDecimal receitaOp = BigDecimal.ZERO;
        BigDecimal custosVar = BigDecimal.ZERO;
        BigDecimal despesasFixas = BigDecimal.ZERO;

        // Itera (for) sobre a lista de movimentos (Critério: Lógica de Agregação - Iteração)
        for (MovementDetail movement : movements) {

            // CORREÇÃO: Status e Grupo estão em MovementHeader, acessado via movement.header()
            String status = movement.header().cStatus(); //
            String group = movement.header().cGrupo();   //
            BigDecimal valor;

            // Filtro de Status (Critério: Ignorar movimentos que não foram pagos/recebidos)
            // CORREÇÃO: Valores (nValRecebido e nValPago) são BigDecimal em MovementSummary, acessado via movement.summary()
            if (GRUPO_CONTA_A_RECEBER.equals(group) && STATUS_RECEBIDO.equals(status)) {
                valor = movement.summary().nValRecebido(); //
            } else if (GRUPO_CONTA_A_PAGAR.equals(group) && STATUS_PAGO.equals(status)) {
                // Para despesas, o valor deve ser positivo para o cálculo,
                // pois o agrupamento (custosVar, despesasFixas) será subtraído depois.
                valor = movement.summary().nValPago(); //
            } else {
                // Ignora movimentos não liquidados ou de outros tipos que não serão agregados
                continue;
            }

            // Pega o cCodCateg (Critério: Obter Categoria Raiz)
            // CORREÇÃO: cCodCateg está em MovementHeader, acessado via movement.header()
            String cCodCateg = movement.header().cCodCateg(); //

            // Chama categoryCache.getRootCategory(...)
            String rootCategory = categoryCacheService.getRootCategory(appKey, appSecret, cCodCateg);

            // Lógica switch (O Cálculo) (Critério: Lógica switch)
            switch (rootCategory) {
                case "1.0": // Receita Operacional
                    receitaOp = receitaOp.add(valor);
                    break;
                case "2.1": // Custos Variáveis
                    custosVar = custosVar.add(valor);
                    break;
                case "3.0": // Despesas Fixas (Incluindo 3.1, 3.2, etc.)
                case "3.1":
                case "3.2":
                    despesasFixas = despesasFixas.add(valor);
                    break;
                // default: ignorar outras categorias (Critério: default: ignorar outras categorias)
                default:
                    break;
            }
        }

        // Finalização (Critério: Após o loop, calcula o resultadoOp)
        // Calcula o Resultado Operacional: Receita Operacional - Custos Variáveis - Despesas Fixas
        BigDecimal resultadoOp = receitaOp.subtract(custosVar).subtract(despesasFixas);

        // Monta e retorna o ReportSimpleDTO completo (Critério: Monta e retorna o ReportSimpleDTO)
        // CORREÇÃO: Ajuste na chamada do construtor de ReportSimpleDTO para incluir todos os 8 campos.
        return new ReportSimpleDTO(
                period, // reportType (usando o período como tipo de relatório)
                "Nome da Empresa Placeholder", // companyName (Placeholder)
                startDate,
                endDate,
                receitaOp,
                custosVar,
                despesasFixas,
                resultadoOp
        );
    }
}
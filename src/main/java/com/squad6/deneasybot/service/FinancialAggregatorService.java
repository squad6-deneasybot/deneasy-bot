package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.Company;
import com.squad6.deneasybot.model.ReportSimpleDTO;
import com.squad6.deneasybot.model.OmieDTO;
import com.squad6.deneasybot.model.OmieDTO.MovementDetail;
import com.squad6.deneasybot.repository.CompanyRepository;
import com.squad6.deneasybot.client.OmieErpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class FinancialAggregatorService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialAggregatorService.class);
    private static final String GRUPO_CONTA_A_PAGAR = "CONTA_A_PAGAR";
    private static final String GRUPO_CONTA_A_RECEBER = "CONTA_A_RECEBER";

    private final MovementFetcherService movementFetcherService;
    private final CategoryCacheService categoryCacheService;
    private final CompanyRepository companyRepository;
    private final OmieErpClient omieErpClient;

    @Autowired
    public FinancialAggregatorService(MovementFetcherService movementFetcherService,
                                      CategoryCacheService categoryCacheService,
                                      CompanyRepository companyRepository, OmieErpClient omieErpClient) {
        this.movementFetcherService = movementFetcherService;
        this.categoryCacheService = categoryCacheService;
        this.companyRepository = companyRepository;
        this.omieErpClient = omieErpClient;
    }

    public ReportSimpleDTO aggregateReportData(String appKey, String appSecret, LocalDate startDate, LocalDate endDate) {

        List<MovementDetail> movements = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret, startDate, endDate);

        BigDecimal receitaOp = BigDecimal.ZERO;
        BigDecimal custosVar = BigDecimal.ZERO;
        BigDecimal despesasFixas = BigDecimal.ZERO;

        logger.info("Iniciando agregação de {} movimentos...", movements.size());

        for (MovementDetail movement : movements) {
            OmieDTO.MovementHeader header = movement.header();
            if (header == null) {
                logger.warn("Movimento com header nulo. Pulando.");
                continue;
            }

            String group = header.cGrupo();
            BigDecimal valor = header.nValorTitulo();

            if (valor == null) continue;

            String cCodCateg = header.cCodCateg();
            if (cCodCateg == null) continue;

            String rootCategory = categoryCacheService.getRootCategory(appKey, appSecret, cCodCateg);

            if (rootCategory == null) {
                logger.warn("Não foi possível mapear a categoria raiz para o código: {}. Pulando.", cCodCateg);
                continue;
            }

            if (GRUPO_CONTA_A_RECEBER.equals(group)) {
                if ("1.0".equals(rootCategory)) {
                    receitaOp = receitaOp.add(valor);
                }
            } else if (GRUPO_CONTA_A_PAGAR.equals(group)) {
                switch (rootCategory) {
                    case "2.1":
                        custosVar = custosVar.add(valor);
                        break;
                    case "3.0":
                    case "3.1":
                    case "3.2":
                        despesasFixas = despesasFixas.add(valor);
                        break;
                    default:
                        if (rootCategory.startsWith("2.") || rootCategory.startsWith("3.")) {
                            despesasFixas = despesasFixas.add(valor);
                        }
                        break;
                }
            }
        }

        String companyName = "Empresa não encontrada";
        try {
            Optional<Company> companyOptional = companyRepository.findByAppKey(appKey);
            if (companyOptional.isPresent()) {
                companyName = companyOptional.get().getName();
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar nome da empresa pelo appKey: {}", appKey, e);
        }

        BigDecimal resultadoOp = receitaOp.subtract(custosVar).subtract(despesasFixas);
        String reportType = "Personalizado";

        return new ReportSimpleDTO(
                reportType,
                companyName,
                startDate,
                endDate,
                receitaOp,
                custosVar,
                despesasFixas,
                resultadoOp
        );
    }

    public BigDecimal getCurrentBalance(String appKey, String appSecret) {
        logger.info("Buscando saldo atual (ObterResumoFinancas)...");
        LocalDate hoje = LocalDate.now();
        OmieDTO.FinancialSummaryResponse response = omieErpClient.getFinancialSummary(appKey, appSecret, hoje);

        if (response != null && response.contaCorrente() != null && response.contaCorrente().vTotal() != null) {
            return response.contaCorrente().vTotal();
        } else {
            return BigDecimal.ZERO;
        }
    }
}
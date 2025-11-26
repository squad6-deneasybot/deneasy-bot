package com.squad6.deneasybot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.squad6.deneasybot.client.OmieErpClient;
import com.squad6.deneasybot.exception.ResourceNotFoundException;
import com.squad6.deneasybot.model.Company;
import com.squad6.deneasybot.model.CategoryStat;
import com.squad6.deneasybot.model.OmieDTO.MovementDetail;
import com.squad6.deneasybot.model.OmieDTO.MovementHeader;
import com.squad6.deneasybot.model.OmieDTO.MovementSummary;
import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.repository.UserRepository;

@Service
public class FaqService {

    private static final Logger logger = LoggerFactory.getLogger(FaqService.class);

    private static final int PROJECTION_DAYS = 7;
    private static final Set<String> STATUS_FECHADO = Set.of("PAGO", "LIQUIDADO", "CANCELADO");
    private static final String GRUPO_CONTA_A_PAGAR = "CONTA_A_PAGAR";
    private static final String GRUPO_CONTA_A_RECEBER = "CONTA_A_RECEBER";

    private final FinancialAggregatorService financialAggregatorService;
    private final MovementFetcherService movementFetcherService;
    private final UserRepository userRepository;
    private final WhatsAppFormatterService formatterService;
    private final CategoryCacheService categoryCacheService;
    private final EncryptionService encryptionService;

    public FaqService(FinancialAggregatorService financialAggregatorService,
                      MovementFetcherService movementFetcherService, UserRepository userRepository,
                      WhatsAppFormatterService formatterService,
                      CategoryCacheService categoryCacheService,
                      EncryptionService encryptionService) {
        this.financialAggregatorService = financialAggregatorService;
        this.movementFetcherService = movementFetcherService;
        this.userRepository = userRepository;
        this.formatterService = formatterService;
        this.categoryCacheService = categoryCacheService;
        this.encryptionService = encryptionService;
    }

    public String getFaqMenu() {
        return formatterService.formatFaqMenu();
    }

    @Transactional(readOnly = true)
    public String getFaqAnswer(String option, String userPhone) {

        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado: " + userPhone));
        Company company = user.getCompany();

        String appKey = encryptionService.decrypt(company.getAppKey());
        String appSecret = encryptionService.decrypt(company.getAppSecret());

        return switch (option.trim()) {
            case "1" -> getTitulosAVencer(appKey, appSecret);
            case "2" -> getTitulosEmAtraso(appKey, appSecret);
            case "3" -> getProjecaoDeCaixa(appKey, appSecret);
            case "4" -> getTopDespesasPorCategoria(appKey, appSecret);
            default -> throw new IllegalArgumentException("Opção de FAQ inválida: " + option);
        };
    }

    private String getProjecaoDeCaixa(String appKey, String appSecret) {
        logger.info("Iniciando projeção de caixa...");

        BigDecimal saldoAtual = financialAggregatorService.getCurrentBalance(appKey, appSecret);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(PROJECTION_DAYS);

        List<MovementDetail> movimentosFuturos = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret,
                startDate, endDate);

        BigDecimal totalPagar = BigDecimal.ZERO;
        BigDecimal totalReceber = BigDecimal.ZERO;

        for (MovementDetail movement : movimentosFuturos) {
            MovementHeader header = movement.header();
            MovementSummary summary = movement.summary();

            if (header == null || summary == null || header.cGrupo() == null ||
                    header.cStatus() == null || summary.nValAberto() == null) {
                continue;
            }

            if (!STATUS_FECHADO.contains(header.cStatus().toUpperCase())) {

                if (GRUPO_CONTA_A_RECEBER.equals(header.cGrupo())) {
                    totalReceber = totalReceber.add(summary.nValAberto());
                } else if (GRUPO_CONTA_A_PAGAR.equals(header.cGrupo())) {
                    totalPagar = totalPagar.add(summary.nValAberto());
                }
            }
        }

        logger.info("Cálculo da projeção ({} dias): SaldoAtual={}, Pagar={}, Receber={}", PROJECTION_DAYS,
                saldoAtual, totalPagar, totalReceber);

        BigDecimal saldoPrevisto = saldoAtual.add(totalReceber).subtract(totalPagar);

        return formatterService.formatFaqProjecaoCaixa(saldoAtual, totalPagar, totalReceber, saldoPrevisto,
                PROJECTION_DAYS);
    }

    private String getTitulosEmAtraso(String appKey, String appSecret) {
        logger.info("Iniciando análise de títulos em atraso...");

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusYears(2);
        LocalDate endDate = today.minusDays(1);

        List<MovementDetail> movimentos = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret,
                startDate, endDate);

        long count1_30 = 0;
        BigDecimal total1_30 = BigDecimal.ZERO;
        long count31_60 = 0;
        BigDecimal total31_60 = BigDecimal.ZERO;
        long count61_90 = 0;
        BigDecimal total61_90 = BigDecimal.ZERO;
        long count90_plus = 0;
        BigDecimal total90_plus = BigDecimal.ZERO;

        for (MovementDetail movement : movimentos) {
            MovementHeader header = movement.header();
            MovementSummary summary = movement.summary();

            if (header == null || summary == null || header.cGrupo() == null || header.cStatus() == null
                    || header.dDtVenc() == null || summary.nValAberto() == null) {
                logger.warn("Pulando movimento com dados faltantes.");
                continue;
            }

            if (!GRUPO_CONTA_A_PAGAR.equals(header.cGrupo()) || STATUS_FECHADO.contains(header.cStatus().toUpperCase())) {
                continue;
            }

            try {
                LocalDate vencimento = LocalDate.parse(header.dDtVenc(), OmieErpClient.OMIE_DATE_FORMATTER);

                if (vencimento.isBefore(today)) {
                    long daysOverdue = ChronoUnit.DAYS.between(vencimento, today);
                    BigDecimal valor = summary.nValAberto();

                    if (daysOverdue <= 30) {
                        count1_30++;
                        total1_30 = total1_30.add(valor);
                    } else if (daysOverdue <= 60) {
                        count31_60++;
                        total31_60 = total31_60.add(valor);
                    } else if (daysOverdue <= 90) {
                        count61_90++;
                        total61_90 = total61_90.add(valor);
                    } else {
                        count90_plus++;
                        total90_plus = total90_plus.add(valor);
                    }
                }
            } catch (Exception e) {
                logger.error("Erro ao processar data de vencimento: {}", header.dDtVenc(), e);
            }
        }

        logger.info("Agregação concluída. Títulos em atraso: 1-30={}, 31-60={}, 61-90={}, >90={}",
                count1_30, count31_60, count61_90, count90_plus);

        return formatterService.formatFaqTitulosEmAtraso(count1_30, total1_30, count31_60, total31_60, count61_90,
                total61_90, count90_plus, total90_plus);
    }

    private String getTopDespesasPorCategoria(String appKey, String appSecret) {
        logger.info("Iniciando busca 'Top 3 Despesas'...");

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        List<MovementDetail> movements = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret, startDate,
                endDate);

        final Set<String> statusPago = Set.of("PAGO", "LIQUIDADO");
        Map<String, BigDecimal> aggregationMap = new HashMap<>();

        for (MovementDetail movement : movements) {
            MovementHeader header = movement.header();
            MovementSummary summary = movement.summary();

            if (header == null || summary == null || header.cGrupo() == null || header.cStatus() == null
                    || header.cCodCateg() == null || summary.nValPago() == null) {
                continue;
            }

            if (GRUPO_CONTA_A_PAGAR.equals(header.cGrupo()) && statusPago.contains(header.cStatus().toUpperCase())) {
                String categoryCode = header.cCodCateg();
                BigDecimal value = summary.nValPago();
                aggregationMap.merge(categoryCode, value, BigDecimal::add);
            }
        }

        List<Map.Entry<String, BigDecimal>> sortedList = aggregationMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .toList();

        List<CategoryStat> topCategories = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sortedList) {
            String categoryCode = entry.getKey();
            BigDecimal totalValue = entry.getValue();

            String categoryName = categoryCacheService.getRootCategory(appKey, appSecret, categoryCode);

            if (categoryName == null || categoryName.isBlank()) {
                categoryName = categoryCode;
                logger.warn("Não foi possível encontrar o nome da categoria para o código: {}. Usando o código.", categoryCode);
            }

            topCategories.add(new CategoryStat(categoryName, totalValue));
        }

        logger.info("Top despesas ({}): {}", sortedList.size(), topCategories);

        return formatterService.formatFaqTopCategorias(topCategories);
    }

    private String getTitulosAVencer(String appKey, String appSecret) {
        logger.info("Iniciando busca 'Títulos a Vencer'...");

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(PROJECTION_DAYS);

        List<MovementDetail> movements = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret, startDate, endDate);

        BigDecimal totalPagar = BigDecimal.ZERO;
        int countPagar = 0;
        BigDecimal totalReceber = BigDecimal.ZERO;
        int countReceber = 0;

        for (MovementDetail movement : movements) {
            MovementHeader header = movement.header();
            MovementSummary summary = movement.summary();

            if (header == null || summary == null || header.cGrupo() == null ||
                    header.cStatus() == null || summary.nValAberto() == null) {
                continue;
            }

            if (!STATUS_FECHADO.contains(header.cStatus().toUpperCase())) {

                if (GRUPO_CONTA_A_PAGAR.equals(header.cGrupo())) {
                    totalPagar = totalPagar.add(summary.nValAberto());
                    countPagar++;
                }
                else if (GRUPO_CONTA_A_RECEBER.equals(header.cGrupo())) {
                    totalReceber = totalReceber.add(summary.nValAberto());
                    countReceber++;
                }
            }
        }

        logger.info("Títulos a vencer ({} dias): Pagar={}, Receber={}", PROJECTION_DAYS, countPagar, countReceber);

        return formatterService.formatFaqTitulosAVencer(countPagar, totalPagar, countReceber, totalReceber, PROJECTION_DAYS);
    }
}
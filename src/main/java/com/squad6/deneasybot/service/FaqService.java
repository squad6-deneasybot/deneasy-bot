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
import com.squad6.deneasybot.model.OmieDTO;
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

    public FaqService(FinancialAggregatorService financialAggregatorService,
                      MovementFetcherService movementFetcherService, UserRepository userRepository,
                      WhatsAppFormatterService formatterService,
                      CategoryCacheService categoryCacheService) {
        this.financialAggregatorService = financialAggregatorService;
        this.movementFetcherService = movementFetcherService;
        this.userRepository = userRepository;
        this.formatterService = formatterService;
        this.categoryCacheService = categoryCacheService;
    }

    @Transactional(readOnly = true)
    public String getProjecaoDeCaixa(String userPhone) {
        logger.info("Iniciando projeção de caixa para {}", userPhone);

        User user = userRepository.findByPhone(userPhone).orElseThrow(() -> {
            logger.error("Usuário {} não encontrado no banco para projeção.", userPhone);
            return new RuntimeException("Usuário não encontrado para projeção.");
        });

        Company company = user.getCompany();
        String appKey = company.getAppKey();
        String appSecret = company.getAppSecret();

        BigDecimal saldoAtual = financialAggregatorService.getCurrentBalance(appKey, appSecret);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(PROJECTION_DAYS);

        logger.info("Buscando movimentos futuros de {} até {} para {}", startDate, endDate, userPhone);
        List<MovementDetail> movimentosFuturos = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret,
                startDate, endDate);

        BigDecimal totalPagar = BigDecimal.ZERO;
        BigDecimal totalReceber = BigDecimal.ZERO;

        for (MovementDetail movement : movimentosFuturos) {
            MovementHeader header = movement.header();
            if (header == null || header.nValorTitulo() == null || header.cGrupo() == null) {
                continue;
            }

            boolean emAberto = header.dDtPagamento() == null || header.dDtPagamento().isBlank();

            if (emAberto) {
                if ("CONTA_A_RECEBER".equals(header.cGrupo())) {
                    totalReceber = totalReceber.add(header.nValorTitulo());
                } else if ("CONTA_A_PAGAR".equals(header.cGrupo())) {
                    totalPagar = totalPagar.add(header.nValorTitulo());
                }
            }
        }
        logger.info("Cálculo da projeção ({} dias) para {}: SaldoAtual={}, Pagar={}, Receber={}", PROJECTION_DAYS,
                userPhone, saldoAtual, totalPagar, totalReceber);

        BigDecimal saldoPrevisto = saldoAtual.add(totalReceber).subtract(totalPagar);

        return formatterService.formatFaqProjecaoCaixa(saldoAtual, totalPagar, totalReceber, saldoPrevisto,
                PROJECTION_DAYS);
    }

    @Transactional(readOnly = true)
    public String getTitulosEmAtraso(String userPhone) {
        logger.info("Iniciando análise de títulos em atraso para {}", userPhone);

        User user = userRepository.findByPhone(userPhone).orElseThrow(() -> {
            logger.error("Usuário {} não encontrado no banco para análise de títulos em atraso.", userPhone);
            return new RuntimeException("Usuário não encontrado para análise de títulos em atraso.");
        });

        Company company = user.getCompany();
        String appKey = company.getAppKey();
        String appSecret = company.getAppSecret();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusYears(2);
        LocalDate endDate = today.minusDays(1);

        logger.info("Buscando movimentos de {} a {} para {}", startDate, endDate, userPhone);
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

        final List<String> statusNaoEmAberto = List.of("PAGO", "LIQUIDADO", "CANCELADO");
        final String GRUPO_CONTA_A_PAGAR = "CONTA_A_PAGAR";

        for (MovementDetail movement : movimentos) {
            MovementHeader header = movement.header();
            OmieDTO.MovementSummary summary = movement.summary();

            if (header == null || summary == null || header.cGrupo() == null || header.cStatus() == null
                    || header.dDtVenc() == null || summary.nValAberto() == null) {
                logger.warn("Pulando movimento com dados faltantes.");
                continue;
            }

            if (!GRUPO_CONTA_A_PAGAR.equals(header.cGrupo())) {
                continue;
            }

            if (statusNaoEmAberto.contains(header.cStatus())) {
                continue;
            }

            try {
                LocalDate vencimento = LocalDate.parse(header.dDtVenc(), OmieErpClient.OMIE_DATE_FORMATTER);

                if (!vencimento.isBefore(today)) {
                    continue;
                }

                long daysOverdue = ChronoUnit.DAYS.between(vencimento, today);
                BigDecimal valor = summary.nValAberto();

                if (daysOverdue >= 1 && daysOverdue <= 30) {
                    count1_30++;
                    total1_30 = total1_30.add(valor);
                } else if (daysOverdue >= 31 && daysOverdue <= 60) {
                    count31_60++;
                    total31_60 = total31_60.add(valor);
                } else if (daysOverdue >= 61 && daysOverdue <= 90) {
                    count61_90++;
                    total61_90 = total61_90.add(valor);
                } else if (daysOverdue > 90) {
                    count90_plus++;
                    total90_plus = total90_plus.add(valor);
                }
            } catch (Exception e) {
                logger.error("Erro ao processar data de vencimento: {} para o usuário {}", header.dDtVenc(), userPhone,
                        e);
            }
        }

        logger.info("Agregação concluída para {}. Títulos em atraso: 1-30={}, 31-60={}, 61-90={}, >90={}", userPhone,
                count1_30, count31_60, count61_90, count90_plus);

        return formatterService.formatFaqTitulosEmAtraso(count1_30, total1_30, count31_60, total31_60, count61_90,
                total61_90, count90_plus, total90_plus);
    }

    @Transactional(readOnly = true)
    public String getTopDespesasPorCategoria(String userPhone) {
        logger.info("Iniciando busca 'Top 3 Despesas' para {}", userPhone);

        User user = userRepository.findByPhone(userPhone).orElseThrow(() -> {
            logger.error("Usuário {} não encontrado no banco para Top Despesas.", userPhone);
            return new RuntimeException("Usuário não encontrado para Top Despesas.");
        });
        Company company = user.getCompany();
        String appKey = company.getAppKey();
        String appSecret = company.getAppSecret();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        List<MovementDetail> movements = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret, startDate,
                endDate);

        final List<String> statusPago = List.of("PAGO", "LIQUIDADO");
        Map<String, BigDecimal> aggregationMap = new HashMap<>();

        for (MovementDetail movement : movements) {
            MovementHeader header = movement.header();
            MovementSummary summary = movement.summary();

            if (header == null || summary == null || header.cGrupo() == null || header.cStatus() == null
                    || header.cCodCateg() == null || summary.nValPago() == null) {
                continue;
            }

            if (GRUPO_CONTA_A_PAGAR.equals(header.cGrupo()) && statusPago.contains(header.cStatus())) {
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

        logger.info("Top despesas para {} ({}): {}", userPhone, sortedList.size(), topCategories);

        return formatterService.formatFaqTopCategorias(topCategories);
    }

    @Transactional(readOnly = true)
    public String getTitulosAVencer(String userPhone) {
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado: " + userPhone));
        Company company = user.getCompany();
        String appKey = company.getAppKey();
        String appSecret = company.getAppSecret();

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

            if (header == null || summary == null || header.cGrupo() == null || header.cStatus() == null || header.dDtVenc() == null || summary.nValAberto() == null) {
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

        return formatterService.formatFaqTitulosAVencer(countPagar, totalPagar, countReceber, totalReceber, PROJECTION_DAYS);
    }

    public String getFaqAnswer(String option, String userPhone) {
        return switch (option) {
            case "1" -> getTitulosAVencer(userPhone);
            case "2" -> getTitulosEmAtraso(userPhone);
            case "3" -> getProjecaoDeCaixa(userPhone);
            case "4" -> getTopDespesasPorCategoria(userPhone);
            default -> throw new IllegalArgumentException("Opção inválida");
        };
    }
}
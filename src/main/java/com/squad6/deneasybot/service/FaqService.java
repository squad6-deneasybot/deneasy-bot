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
    // (Nome corrigido de STATUS_EM_ABERTO para STATUS_LIQUIDADO para maior clareza)
    private static final Set<String> STATUS_LIQUIDADO = Set.of("PAGO", "LIQUIDADO", "CANCELADO");
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

    // --- MÉTODO FALTANTE (CA 3) ADICIONADO ---
    /**
     * CA 3: Retorna o menu de FAQ formatado.
     * Chamado pelo MenuService (case "2").
     */
    public String getFaqMenu() {
        // (A lógica de quais perguntas mostrar pode ser baseada no UserProfile no futuro)
        // Por enquanto, chama o formatador com as perguntas hardcoded.
        // O formatador já tem o texto (1. Títulos a Vencer, 2. Títulos Vencidos, etc.)
        return formatterService.formatFaqMenu();
    }

    // --- MÉTODO PRINCIPAL (CA 3) ADICIONADO ---
    /**
     * CA 3: Ponto de entrada do Orquestrador.
     * Chama a lógica de negócio correta com base na opção.
     */
    @Transactional(readOnly = true) // Transação para garantir a busca de credenciais
    public String getFaqAnswer(String option, String userPhone) {

        // Busca o usuário e credenciais UMA VEZ
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado: " + userPhone));
        Company company = user.getCompany();
        String appKey = company.getAppKey();
        String appSecret = company.getAppSecret();

        // Usa o switch moderno
        return switch (option.trim()) {
            // (Note: os métodos de lógica agora recebem appKey/appSecret em vez de userPhone)
            case "1" -> getTitulosAVencer(appKey, appSecret);
            case "2" -> getTitulosEmAtraso(appKey, appSecret);
            case "3" -> getProjecaoDeCaixa(appKey, appSecret);
            case "4" -> getTopDespesasPorCategoria(appKey, appSecret);
            // TODO: Adicionar cases 5-8 aqui
            default -> throw new IllegalArgumentException("Opção de FAQ inválida: " + option);
        };
    }


    // --- MÉTODOS DE LÓGICA DE NEGÓCIO (PRIVADOS) ---

    /**
     * RF-FAQ-03 (Lógica): Projeção de Caixa
     * (MÉTODO CORRIGIDO)
     */
    private String getProjecaoDeCaixa(String appKey, String appSecret) {
        logger.info("Iniciando projeção de caixa...");

        // 1. Saldo Atual (RF-ERP-04)
        BigDecimal saldoAtual = financialAggregatorService.getCurrentBalance(appKey, appSecret);

        // 2. Definir Período
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(PROJECTION_DAYS);

        // 3. Buscar Movimentos Futuros (RF-ERP-01)
        List<MovementDetail> movimentosFuturos = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret,
                startDate, endDate);

        BigDecimal totalPagar = BigDecimal.ZERO;
        BigDecimal totalReceber = BigDecimal.ZERO;

        for (MovementDetail movement : movimentosFuturos) {
            MovementHeader header = movement.header();
            MovementSummary summary = movement.summary(); // <-- CORREÇÃO: Usar o Summary

            // Validação de dados essenciais
            if (header == null || summary == null || header.cGrupo() == null ||
                    header.cStatus() == null || summary.nValAberto() == null) {
                continue;
            }

            // CORREÇÃO: Filtra apenas por status EM ABERTO
            if (!STATUS_LIQUIDADO.contains(header.cStatus().toUpperCase())) {

                // CORREÇÃO: Soma o nValAberto do Summary
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

    /**
     * RF-FAQ-02 (Lógica): Títulos em Atraso
     */
    private String getTitulosEmAtraso(String appKey, String appSecret) {
        logger.info("Iniciando análise de títulos em atraso...");

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusYears(2); // Período retroativo
        LocalDate endDate = today.minusDays(1);  // Até ontem

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

            // Foco apenas em Contas a Pagar que NÃO estão liquidadas
            if (!GRUPO_CONTA_A_PAGAR.equals(header.cGrupo()) || STATUS_LIQUIDADO.contains(header.cStatus().toUpperCase())) {
                continue;
            }

            try {
                LocalDate vencimento = LocalDate.parse(header.dDtVenc(), OmieErpClient.OMIE_DATE_FORMATTER);

                // Garante que estamos olhando apenas para o que venceu ANTES de hoje
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
                    } else { // Mais de 90 dias
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

    /**
     * RF-FAQ-05 (Lógica): Top 3 Despesas por Categoria
     */
    private String getTopDespesasPorCategoria(String appKey, String appSecret) {
        logger.info("Iniciando busca 'Top 3 Despesas'...");

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        List<MovementDetail> movements = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret, startDate,
                endDate);

        // CORREÇÃO: Usando a constante da classe (linha 34)
        final Set<String> statusPago = Set.of("PAGO", "LIQUIDADO");
        Map<String, BigDecimal> aggregationMap = new HashMap<>();

        for (MovementDetail movement : movements) {
            MovementHeader header = movement.header();
            MovementSummary summary = movement.summary();

            if (header == null || summary == null || header.cGrupo() == null || header.cStatus() == null
                    || header.cCodCateg() == null || summary.nValPago() == null) {
                continue;
            }

            // CORREÇÃO: Usando a constante da classe (linha 35)
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

            // (CA 3g)
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

    /**
     * RF-FAQ-01 (Lógica): Títulos a Vencer
     * (MÉTODO CORRIGIDO - Lógica otimizada e sem duplicata)
     */
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
                continue; // Ignora movimentos malformados
            }

            // Filtra por status (não pode estar pago/cancelado)
            if (!STATUS_LIQUIDADO.contains(header.cStatus().toUpperCase())) {

                // Agrega (o Fetcher JÁ filtrou pela data de vencimento)
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
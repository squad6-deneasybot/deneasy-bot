package com.squad6.deneasybot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.squad6.deneasybot.client.OmieErpClient;
import com.squad6.deneasybot.model.Company;
import com.squad6.deneasybot.model.OmieDTO;
import com.squad6.deneasybot.model.OmieDTO.MovementDetail;
import com.squad6.deneasybot.model.OmieDTO.MovementHeader;
import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.repository.UserRepository;

@Service
public class FaqService {

    private static final Logger logger = LoggerFactory.getLogger(FaqService.class);
    private static final int PROJECTION_DAYS = 7;

    private final FinancialAggregatorService financialAggregatorService;
    private final MovementFetcherService movementFetcherService;
    private final UserRepository userRepository;
    private final WhatsAppFormatterService formatterService;

    public FaqService(FinancialAggregatorService financialAggregatorService,
            MovementFetcherService movementFetcherService, UserRepository userRepository,
            WhatsAppFormatterService formatterService) {
        this.financialAggregatorService = financialAggregatorService;
        this.movementFetcherService = movementFetcherService;
        this.userRepository = userRepository;
        this.formatterService = formatterService;
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
}
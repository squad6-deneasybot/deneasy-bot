package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.Company;
import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.OmieDTO;
import com.squad6.deneasybot.model.OmieDTO.MovementDetail;
import com.squad6.deneasybot.model.OmieDTO.MovementHeader;
import com.squad6.deneasybot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class FaqService {

    private static final Logger logger = LoggerFactory.getLogger(FaqService.class);
    private static final int PROJECTION_DAYS = 7;

    private final FinancialAggregatorService financialAggregatorService;
    private final MovementFetcherService movementFetcherService;
    private final UserRepository userRepository;
    private final WhatsAppFormatterService formatterService;

    public FaqService(FinancialAggregatorService financialAggregatorService,
                      MovementFetcherService movementFetcherService,
                      UserRepository userRepository,
                      WhatsAppFormatterService formatterService) {
        this.financialAggregatorService = financialAggregatorService;
        this.movementFetcherService = movementFetcherService;
        this.userRepository = userRepository;
        this.formatterService = formatterService;
    }

    @Transactional(readOnly = true)
    public String getProjecaoDeCaixa(String userPhone) {
        logger.info("Iniciando projeção de caixa para {}", userPhone);

        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> {
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
        List<MovementDetail> movimentosFuturos = movementFetcherService.fetchAllMovementsForPeriod(appKey, appSecret, startDate, endDate);

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
        logger.info("Cálculo da projeção ({} dias) para {}: SaldoAtual={}, Pagar={}, Receber={}",
                PROJECTION_DAYS, userPhone, saldoAtual, totalPagar, totalReceber);

        BigDecimal saldoPrevisto = saldoAtual.add(totalReceber).subtract(totalPagar);

        return formatterService.formatFaqProjecaoCaixa(saldoAtual, totalPagar, totalReceber, saldoPrevisto, PROJECTION_DAYS);
    }
}
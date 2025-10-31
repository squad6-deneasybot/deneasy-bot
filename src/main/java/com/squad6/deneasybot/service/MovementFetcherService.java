package com.squad6.deneasybot.service;

import com.squad6.deneasybot.client.OmieErpClient;
import com.squad6.deneasybot.model.OmieDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovementFetcherService {

    private static final Logger logger = LoggerFactory.getLogger(MovementFetcherService.class);
    private final OmieErpClient omieErpClient;

    public MovementFetcherService(OmieErpClient omieErpClient) {
        this.omieErpClient = omieErpClient;
    }

    public List<OmieDTO.MovimentoDetalhe> fetchAllMovementsForPeriod(String appKey, String appSecret, LocalDate startDate, LocalDate endDate) {

        List<OmieDTO.MovimentoDetalhe> allMovements = new ArrayList<>();
        int currentPage = 1;
        int totalPages = 1;

        logger.info("Iniciando busca de movimentos para o período de {} a {}", startDate, endDate);

        do {
            logger.debug("Buscando página {} de {}", currentPage, totalPages);

            OmieDTO.MovementResponse response = omieErpClient.listMovements(
                    appKey,
                    appSecret,
                    currentPage,
                    startDate,
                    endDate
            );

            if (response == null) {
                logger.warn("Resposta da API Omie (ListarMovimentos) foi nula para a página {}.", currentPage);
                break;
            }

            if (response.nTotRegistros() == 0) {
                logger.info("Nenhum registro encontrado para o período.");
                break;
            }

            if (response.movimento() != null && !response.movimento().isEmpty()) {
                allMovements.addAll(response.movimento());
            }

            totalPages = response.nTotPaginas();

            currentPage++;

        } while (currentPage <= totalPages);

        logger.info("Busca concluída. Total de {} movimentos financeiros encontrados.", allMovements.size());
        return allMovements;
    }
}
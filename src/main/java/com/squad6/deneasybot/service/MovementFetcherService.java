package com.squad6.deneasybot.service;

import com.squad6.deneasybot.client.OmieErpClient;
import com.squad6.deneasybot.model.OmieDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MovementFetcherService {

    private static final Logger logger = LoggerFactory.getLogger(MovementFetcherService.class);
    private final OmieErpClient omieErpClient;

    public MovementFetcherService(OmieErpClient omieErpClient) {
        this.omieErpClient = omieErpClient;
    }

    public List<OmieDTO.MovementDetail> fetchAllMovementsForPeriod(String appKey, String appSecret, LocalDate startDate, LocalDate endDate) {


        String dDtVencDe = startDate.format(OmieErpClient.OMIE_DATE_FORMATTER);
        String dDtVencAte = endDate.format(OmieErpClient.OMIE_DATE_FORMATTER);

        logger.info("Iniciando busca de movimentos para o período de {} a {}", dDtVencDe, dDtVencAte);

        List<OmieDTO.MovementDetail> allMovements = new ArrayList<>();
        int currentPage = 1;
        int totalPages = 1;

        do {
            logger.debug("Buscando página {} de {}", currentPage, totalPages);


            var param = new OmieDTO.MovementFilterParam(
                    currentPage,
                    OmieErpClient.MAX_REGISTROS_POR_PAGINA, // 1000
                    dDtVencDe,
                    dDtVencAte
            );


            OmieDTO.MovementResponse response = omieErpClient.listMovements(appKey, appSecret, param);


            if (response == null) {
                logger.warn("Resposta da API Omie (ListarMovimentos) foi nula para a página {}.", currentPage);
                break;
            }


            if (currentPage == 1 && response.nTotRegistros() == 0) {
                logger.info("Nenhum registro encontrado para o período.");
                return Collections.emptyList();
            }


            if (response.movements() != null && !response.movements().isEmpty()) {
                allMovements.addAll(response.movements());
            }


            totalPages = response.nTotPaginas();


            currentPage++;

        } while (currentPage <= totalPages);

        logger.info("Busca concluída. Total de {} movimentos financeiros encontrados em {} páginas.", allMovements.size(), totalPages);
        return allMovements;
    }
}
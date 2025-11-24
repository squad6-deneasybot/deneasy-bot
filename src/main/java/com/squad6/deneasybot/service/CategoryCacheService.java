package com.squad6.deneasybot.service;

import com.squad6.deneasybot.client.OmieErpClient;
import com.squad6.deneasybot.model.OmieDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CategoryCacheService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryCacheService.class);
    private final OmieErpClient omieErpClient;

    private final Map<String, Map<String, String>> companyCategoryCache = new ConcurrentHashMap<>();

    public CategoryCacheService(OmieErpClient omieErpClient) {
        this.omieErpClient = omieErpClient;
    }

    public String getRootCategory(String appKey, String appSecret, String categoryCode) {

        if (!companyCategoryCache.containsKey(appKey)) {
            synchronized (this) {
                if (!companyCategoryCache.containsKey(appKey)) {
                    loadAllCategoriesFromErp(appKey, appSecret);
                }
            }
        }

        Map<String, String> categoryMap = companyCategoryCache.get(appKey);
        if (categoryMap == null) {
            return categoryCode;
        }
        return categoryMap.getOrDefault(categoryCode, categoryCode);
    }

    private void loadAllCategoriesFromErp(String appKey, String appSecret) {
        logger.info("Iniciando pré-carregamento de categorias para a empresa (AppKey: {})...", appKey);

        Map<String, String> newCache = new HashMap<>();
        int page = 1;
        int totalPages = 1;

        do {
            try {
                OmieDTO.CategoryListResponse response = omieErpClient.listCategories(appKey, appSecret, page);

                if (response != null) {
                    totalPages = response.totalDePaginas();

                    if (response.categorias() != null) {
                        for (OmieDTO.OmieCategoryDTO cat : response.categorias()) {
                            String rootName = extractRootName(cat.descricao());

                            newCache.put(cat.codigo(), rootName);
                        }
                    }
                }
                page++;

            } catch (Exception e) {
                logger.error("Erro ao carregar página {} de categorias. Parando carga.", page, e);
                break;
            }
        } while (page <= totalPages);

        companyCategoryCache.put(appKey, newCache);
        logger.info("Cache de categorias carregado com sucesso. Total mapeado: {}", newCache.size());
    }

    private String extractRootName(String descricao) {
        if (descricao == null || descricao.isBlank()) return "Indefinido";

        String[] parts = descricao.trim().split(" ");

        if (parts.length > 0) {
            String potentialCode = parts[0];
            if (potentialCode.matches(".*\\d.*")) {
                return potentialCode;
            }
        }
        return descricao;
    }

    public void clearCache(String appKey) {
        companyCategoryCache.remove(appKey);
    }
}
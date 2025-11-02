package com.squad6.deneasybot.service;

import com.squad6.deneasybot.client.OmieErpClient;
import com.squad6.deneasybot.model.OmieDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CategoryCacheService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryCacheService.class);
    private final OmieErpClient omieErpClient;

    private final Map<String, String> categoryCache = new ConcurrentHashMap<>();

    public CategoryCacheService(OmieErpClient omieErpClient) {
        this.omieErpClient = omieErpClient;
    }

    private String parseRootCategory(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            return null;
        }

        String[] parts = descricao.split(" ", 2);
        String rootCategory = parts[0];

        if (rootCategory.matches("^[\\d.]+$")) {
            return rootCategory;
        } else {
            logger.warn("Formato de descrição de categoria inesperado: '{}'", descricao);
            return null;
        }
    }

    public String getRootCategory(String appKey, String appSecret, String subCategoryCode) {
        if (categoryCache.containsKey(subCategoryCode)) {
            logger.debug("Cache HIT para categoria: {}", subCategoryCode);
            return categoryCache.get(subCategoryCode);
        }

        logger.info("Cache MISS para categoria: {}. Consultando API Omie...", subCategoryCode);

        OmieDTO.OmieCategoryDTO categoryDTO = omieErpClient.consultCategory(appKey, appSecret, subCategoryCode);

        String rootCategory = parseRootCategory(categoryDTO.descricao());

        if (rootCategory != null) {
            categoryCache.put(subCategoryCode, rootCategory);
            logger.info("Categoria '{}' mapeada para '{}' e salva no cache.", subCategoryCode, rootCategory);
            return rootCategory;
        } else {
            logger.warn("Não foi possível extrair a categoria raiz da descrição: '{}' para o código: {}",
                    categoryDTO.descricao(), subCategoryCode);
            return null;
        }
    }
}

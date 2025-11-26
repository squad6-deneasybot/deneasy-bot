package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ChatState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatStateService {

    private static final Logger logger = LoggerFactory.getLogger(ChatStateService.class);

    private final Map<String, ChatState> userStates = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> userSessionData = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActivityTimestamp = new ConcurrentHashMap<>();

    private static final long INACTIVITY_THRESHOLD_MS = 3600000;

    public ChatState getState(String userId){
        return userStates.getOrDefault(userId, ChatState.START);
    }

    public void setState(String userId, ChatState state){
        userStates.put(userId, state);
        lastActivityTimestamp.put(userId, System.currentTimeMillis());
    }

    public void saveData(String userId, String key, Object value){
        userSessionData.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(key, value);
        lastActivityTimestamp.put(userId, System.currentTimeMillis());
    }

    public <T> Optional<T> getData(String userId, String key, Class<T> type){
        Map<String, Object> data = userSessionData.get(userId);
        if (data != null && data.get(key) != null){
            return Optional.of(type.cast(data.get(key)));
        }
        return Optional.empty();
    }

    public void clearData(String userId){
        userSessionData.remove(userId);
    }

    public void clearAll(String userId){
        userStates.remove(userId);
        userSessionData.remove(userId);
        lastActivityTimestamp.remove(userId);
    }

    @Scheduled(fixedRate = 1800000)
    public void cleanupInactiveSessions() {
        logger.info("Iniciando job de limpeza de sessões inativas...");
        long now = System.currentTimeMillis();

        List<String> inactiveUsers = lastActivityTimestamp.entrySet().stream()
                .filter(entry -> (now - entry.getValue()) > INACTIVITY_THRESHOLD_MS)
                .map(Map.Entry::getKey)
                .toList();

        if (inactiveUsers.isEmpty()) {
            logger.info("Nenhuma sessão inativa encontrada.");
            return;
        }

        for (String userId : inactiveUsers) {
            clearAll(userId);
            logger.info("Sessão inativa limpa para o usuário: {}", userId);
        }

        logger.info("Job de limpeza concluído. Total removido: {}", inactiveUsers.size());
    }
}

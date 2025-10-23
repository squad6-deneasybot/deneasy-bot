package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ChatState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatStateService {
    private final Map<String, ChatState> userStates = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> userSessionData = new ConcurrentHashMap<>();

    public ChatState getState(String userId){
        return userStates.getOrDefault(userId, ChatState.START);
    }

    public void setState(String userId, ChatState state){
        userStates.put(userId, state);
    }

    public void saveData(String userId, String key, Object value){
        userSessionData.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(key, value);
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
    }
}

package com.squad6.deneasybot.model;

import java.time.LocalDateTime;

public record WishlistDTO(
        Long id,
        String content,
        LocalDateTime createdAt,
        String userName,
        String companyName
) {
}
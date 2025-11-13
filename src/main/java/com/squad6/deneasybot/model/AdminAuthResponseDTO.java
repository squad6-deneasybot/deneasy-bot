package com.squad6.deneasybot.model;

public record AdminAuthResponseDTO(
        Long id,
        String name,
        String email,
        String jwt
) {
}
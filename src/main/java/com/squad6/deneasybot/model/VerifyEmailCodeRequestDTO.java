package com.squad6.deneasybot.model;

public record VerifyEmailCodeRequestDTO(String tokenHash, Context context, UserDTO user) {}
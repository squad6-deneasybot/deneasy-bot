package com.squad6.deneasybot.model;

public class VerifyEmailCodeResponseDTO {
    private UserDTO user;
    private String jwt;

    public VerifyEmailCodeResponseDTO(UserDTO user, String jwt) {
        this.user = user;
        this.jwt = jwt;
    }

    public UserDTO getUser() { return user; }
    public String getJwt() { return jwt; }
}

package com.squad6.deneasybot.model;

public class VerifyEmailCodeRequestDTO {
    private String tokenHash;
    private Context context;
    private UserDTO user;

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Context getContext() { return context; }
    public void setContext(Context context) { this.context = context; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
}

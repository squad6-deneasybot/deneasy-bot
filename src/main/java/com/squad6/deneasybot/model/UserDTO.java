package com.squad6.deneasybot.model;

public class UserDTO {
    private Long id;
    private Long companyId;
    private String name;
    private String phone;
    private String email;
    private UserProfile profile;
    private String session_token;

    public UserDTO() {}

    public UserDTO(User user) {
        this.id = user.getId();
        if (user.getCompany() != null) {
            this.companyId = user.getCompany().getId();
        }
        this.name = user.getName();
        this.phone = user.getPhone();
        this.email = user.getEmail();
        this.profile = user.getProfile();
        if (user.getSessionToken() != null) {
            this.session_token = user.getSessionToken();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }

    public String getSessionToken() { return session_token; }
    public void setSessionToken(String sessionToken) { this.session_token = sessionToken; }
}
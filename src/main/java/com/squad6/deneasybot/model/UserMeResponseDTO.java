package com.squad6.deneasybot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserMeResponseDTO {

    private Long id;
    private String name;
    private String phone;
    private String email;

    @JsonProperty("company_id")
    private Long companyId;

    private UserProfile profile;

    public UserMeResponseDTO() {
    }

    public UserMeResponseDTO(Long id, String name, String phone, String email, Long companyId, UserProfile profile) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.companyId = companyId;
        this.profile = profile;
    }

    public static UserMeResponseDTO fromEntity(User user) {
        Long cId = (user.getCompany() != null) ? user.getCompany().getId() : null;

        return new UserMeResponseDTO(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                cId,
                user.getProfile()
        );
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }
}
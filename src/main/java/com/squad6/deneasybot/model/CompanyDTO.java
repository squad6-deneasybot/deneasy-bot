package com.squad6.deneasybot.model;

public class CompanyDTO {
    private String companyName;
    private String appKey;
    private String appSecret;

    public CompanyDTO() {}

    public CompanyDTO(Company company) {
        this.companyName = company.getName();
        this.appKey = company.getAppKey();
        this.appSecret = company.getAppSecret();
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }
}
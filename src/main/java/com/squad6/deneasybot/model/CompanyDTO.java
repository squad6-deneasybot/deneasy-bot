package com.squad6.deneasybot.model;

public class CompanyDTO {
    private Long id;
    private String companyName;
    private String appKey;
    private String appSecret;

    private Long managerId;
    private String managerName;
    private String managerEmail;
    private String managerPhone;

    public CompanyDTO() {}

    public CompanyDTO(Long id, String companyName, String appKey, String appSecret,
                      Long managerId, String managerName, String managerEmail, String managerPhone) {
        this.id = id;
        this.companyName = companyName;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.managerId = managerId;
        this.managerName = managerName;
        this.managerEmail = managerEmail;
        this.managerPhone = managerPhone;
    }

    public CompanyDTO(Company company) {
        this.id = company.getId();
        this.companyName = company.getName();
        this.appKey = company.getAppKey();
        this.appSecret = company.getAppSecret();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getManagerEmail() { return managerEmail; }
    public void setManagerEmail(String managerEmail) { this.managerEmail = managerEmail; }

    public String getManagerPhone() { return managerPhone; }
    public void setManagerPhone(String managerPhone) { this.managerPhone = managerPhone; }
}
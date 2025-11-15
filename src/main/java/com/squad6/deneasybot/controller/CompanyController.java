package com.squad6.deneasybot.controller;

import com.squad6.deneasybot.model.Company;
import com.squad6.deneasybot.model.CompanyDTO;
import com.squad6.deneasybot.service.AuthService;
import com.squad6.deneasybot.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/company")
public class CompanyController {

    private final CompanyService companyService;
    private final AuthService authService;

    public CompanyController(CompanyService companyService, AuthService authService) {
        this.companyService = companyService;
        this.authService = authService;
    }

    @PostMapping("/validate")
    public ResponseEntity<CompanyDTO> validateCompany(@RequestBody CompanyDTO request) {
        CompanyDTO validatedCompany = authService.validateCompany(request.getAppKey(), request.getAppSecret());
        return ResponseEntity.ok(validatedCompany);
    }

    @GetMapping
    public ResponseEntity<List<CompanyDTO>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyDTO> getCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    @PostMapping
    public ResponseEntity<Company> createCompany(@RequestBody CompanyDTO company) {
        Company newCompany = companyService.createCompany(company);
        return ResponseEntity.ok(newCompany);
    }


    @PutMapping("/{id}")
    public ResponseEntity<CompanyDTO> updateCompany(@PathVariable Long id, @RequestBody CompanyDTO company) {
        CompanyDTO updatedDto = companyService.updateCompany(id, company);
        return ResponseEntity.ok(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

}
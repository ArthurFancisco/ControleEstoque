package br.com.flowstock.web.controller;

import br.com.flowstock.domain.enums.CompanyStatus;
import br.com.flowstock.service.AuditService;
import br.com.flowstock.service.SuperAdminService;
import br.com.flowstock.web.dto.CommonDtos.AuditLogResponse;
import br.com.flowstock.web.dto.CommonDtos.CompanyOnboardingRequest;
import br.com.flowstock.web.dto.CommonDtos.CompanyRequest;
import br.com.flowstock.web.dto.CommonDtos.CompanyResponse;
import br.com.flowstock.web.dto.CommonDtos.HealthResponse;
import br.com.flowstock.web.dto.CommonDtos.PlanResponse;
import br.com.flowstock.web.dto.CommonDtos.PlanUpdateRequest;
import br.com.flowstock.web.dto.CommonDtos.SuperAdminDashboardResponse;
import br.com.flowstock.web.dto.CommonDtos.UserCreateRequest;
import br.com.flowstock.web.dto.CommonDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {
    private final SuperAdminService superAdminService;
    private final AuditService auditService;

    public SuperAdminController(SuperAdminService superAdminService, AuditService auditService) {
        this.superAdminService = superAdminService;
        this.auditService = auditService;
    }

    @GetMapping("/dashboard")
    public SuperAdminDashboardResponse dashboard() {
        return superAdminService.dashboard();
    }

    @GetMapping("/companies")
    public List<CompanyResponse> companies() {
        return superAdminService.companies();
    }

    @PostMapping("/companies")
    public CompanyResponse createCompany(@Valid @RequestBody CompanyOnboardingRequest request) {
        return superAdminService.onboardCompany(request);
    }

    @PutMapping("/companies/{id}")
    public CompanyResponse updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        return superAdminService.updateCompany(id, request);
    }

    @PatchMapping("/companies/{id}/status")
    public CompanyResponse changeStatus(@PathVariable Long id, @RequestParam CompanyStatus status) {
        return superAdminService.changeStatus(id, status);
    }

    @PatchMapping("/companies/{companyId}/plan")
    public CompanyResponse changePlan(@PathVariable Long companyId, @RequestParam Long planId) {
        return superAdminService.changePlan(companyId, planId);
    }

    @PatchMapping("/companies/{companyId}/extend-trial")
    public CompanyResponse extendTrial(@PathVariable Long companyId, @RequestParam(defaultValue = "7") int days) {
        return superAdminService.extendTrial(companyId, days);
    }

    @GetMapping("/companies/{companyId}/users")
    public List<UserResponse> users(@PathVariable Long companyId) {
        return superAdminService.usersByCompany(companyId);
    }

    @PostMapping("/companies/{companyId}/users")
    public UserResponse createUser(@PathVariable Long companyId, @Valid @RequestBody UserCreateRequest request) {
        return superAdminService.createCompanyAdmin(companyId, request);
    }

    @PatchMapping("/companies/{companyId}/users/{userId}/active")
    public UserResponse setUserActive(@PathVariable Long companyId, @PathVariable Long userId, @RequestParam boolean active) {
        return superAdminService.setUserActive(companyId, userId, active);
    }

    @GetMapping("/plans")
    public List<PlanResponse> plans() {
        return superAdminService.plans();
    }

    @PutMapping("/plans/{id}")
    public PlanResponse updatePlan(@PathVariable Long id, @RequestBody PlanUpdateRequest request) {
        return superAdminService.updatePlan(id, request);
    }

    @GetMapping("/logs")
    public List<AuditLogResponse> logs() {
        return auditService.latestGlobal();
    }

    @GetMapping("/health")
    public List<HealthResponse> health() {
        return superAdminService.health();
    }
}

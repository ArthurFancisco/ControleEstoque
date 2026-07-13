package br.com.flowstock.service;

import br.com.flowstock.domain.entity.Company;
import br.com.flowstock.domain.entity.Plan;
import br.com.flowstock.domain.entity.User;
import br.com.flowstock.domain.enums.CompanyStatus;
import br.com.flowstock.domain.enums.PaymentStatus;
import br.com.flowstock.domain.enums.UserRole;
import br.com.flowstock.repository.CompanyRepository;
import br.com.flowstock.repository.PlanRepository;
import br.com.flowstock.repository.SubscriptionRepository;
import br.com.flowstock.repository.SystemHealthLogRepository;
import br.com.flowstock.repository.UserRepository;
import br.com.flowstock.web.dto.CommonDtos.CompanyOnboardingRequest;
import br.com.flowstock.web.dto.CommonDtos.CompanyRequest;
import br.com.flowstock.web.dto.CommonDtos.CompanyResponse;
import br.com.flowstock.web.dto.CommonDtos.HealthResponse;
import br.com.flowstock.web.dto.CommonDtos.PlanResponse;
import br.com.flowstock.web.dto.CommonDtos.PlanUpdateRequest;
import br.com.flowstock.web.dto.CommonDtos.SuperAdminDashboardResponse;
import br.com.flowstock.web.dto.CommonDtos.UserCreateRequest;
import br.com.flowstock.web.dto.CommonDtos.UserResponse;
import br.com.flowstock.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class SuperAdminService {
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SystemHealthLogRepository healthLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final DtoMapper mapper;

    public SuperAdminService(CompanyRepository companyRepository, PlanRepository planRepository, UserRepository userRepository,
                             SubscriptionRepository subscriptionRepository, SystemHealthLogRepository healthLogRepository,
                             PasswordEncoder passwordEncoder, CurrentUserService currentUserService,
                             AuditService auditService, DtoMapper mapper) {
        this.companyRepository = companyRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.healthLogRepository = healthLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public SuperAdminDashboardResponse dashboard() {
        long active = companyRepository.countByStatus(CompanyStatus.ACTIVE);
        long trial = companyRepository.countByStatus(CompanyStatus.TRIAL);
        long suspended = companyRepository.countByStatus(CompanyStatus.SUSPENDED);
        BigDecimal expectedRevenue = companyRepository.findAll().stream()
            .filter(c -> c.getStatus() == CompanyStatus.ACTIVE || c.getStatus() == CompanyStatus.TRIAL || c.getStatus() == CompanyStatus.PAST_DUE)
            .map(c -> c.getPlan().getPrice())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long overdue = subscriptionRepository.countByPaymentStatus(PaymentStatus.OVERDUE);
        return new SuperAdminDashboardResponse(active, trial, suspended, expectedRevenue, overdue, 0, "ONLINE");
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> companies() {
        return companyRepository.findAll().stream().map(mapper::company).toList();
    }

    @Transactional
    public CompanyResponse onboardCompany(CompanyOnboardingRequest request) {
        userRepository.findByEmailIgnoreCase(request.adminEmail()).ifPresent(u -> {
            throw new ApiException(HttpStatus.CONFLICT, "Email do administrador ja cadastrado.");
        });

        Company company = new Company();
        applyCompany(company, new CompanyRequest(
            request.name(),
            request.document(),
            request.email(),
            request.phone(),
            request.planId(),
            request.status(),
            request.trialEndsAt(),
            request.subscriptionEndsAt()
        ));
        Company savedCompany = companyRepository.save(company);

        User admin = new User();
        admin.setCompany(savedCompany);
        admin.setName(request.adminName());
        admin.setEmail(request.adminEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        admin.setRole(UserRole.COMPANY_ADMIN);
        admin.setActive(true);
        User savedAdmin = userRepository.save(admin);

        User actor = currentUserService.currentUser();
        auditService.record(savedCompany, actor, "COMPANY_CREATED", "Company", savedCompany.getId(), null, savedCompany.getName(), null);
        auditService.record(savedCompany, actor, "COMPANY_ADMIN_CREATED", "User", savedAdmin.getId(), null, savedAdmin.getEmail(), null);
        return mapper.company(savedCompany);
    }

    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {
        Company company = new Company();
        applyCompany(company, request);
        Company saved = companyRepository.save(company);
        auditService.record(saved, currentUserService.currentUser(), "COMPANY_CREATED", "Company", saved.getId(), null, saved.getName(), null);
        return mapper.company(saved);
    }

    @Transactional
    public CompanyResponse updateCompany(Long id, CompanyRequest request) {
        Company company = findCompany(id);
        String oldValue = company.getName() + "/" + company.getStatus();
        applyCompany(company, request);
        company.setUpdatedAt(Instant.now());
        Company saved = companyRepository.save(company);
        auditService.record(saved, currentUserService.currentUser(), "COMPANY_UPDATED", "Company", id, oldValue, saved.getName() + "/" + saved.getStatus(), null);
        return mapper.company(saved);
    }

    @Transactional
    public CompanyResponse changeStatus(Long id, CompanyStatus status) {
        Company company = findCompany(id);
        CompanyStatus old = company.getStatus();
        company.setStatus(status);
        company.setUpdatedAt(Instant.now());
        auditService.record(company, currentUserService.currentUser(), "COMPANY_STATUS_CHANGED", "Company", id, old.name(), status.name(), null);
        return mapper.company(company);
    }

    @Transactional
    public CompanyResponse changePlan(Long companyId, Long planId) {
        Company company = findCompany(companyId);
        Plan plan = planRepository.findById(planId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plano nao encontrado."));
        String old = company.getPlan().getName();
        company.setPlan(plan);
        company.setUpdatedAt(Instant.now());
        auditService.record(company, currentUserService.currentUser(), "COMPANY_PLAN_CHANGED", "Company", companyId, old, plan.getName(), null);
        return mapper.company(company);
    }

    @Transactional
    public CompanyResponse extendTrial(Long companyId, int days) {
        Company company = findCompany(companyId);
        LocalDate base = company.getTrialEndsAt() == null ? LocalDate.now() : company.getTrialEndsAt();
        company.setTrialEndsAt(base.plusDays(days));
        company.setUpdatedAt(Instant.now());
        auditService.record(company, currentUserService.currentUser(), "COMPANY_TRIAL_EXTENDED", "Company", companyId, String.valueOf(base), String.valueOf(company.getTrialEndsAt()), null);
        return mapper.company(company);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> usersByCompany(Long companyId) {
        return userRepository.findByCompanyIdOrderByName(companyId).stream().map(mapper::user).toList();
    }

    @Transactional
    public UserResponse createCompanyAdmin(Long companyId, UserCreateRequest request) {
        if (request.role() == UserRole.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Use um usuario de empresa para esta acao.");
        }
        Company company = findCompany(companyId);
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(u -> {
            throw new ApiException(HttpStatus.CONFLICT, "Email ja cadastrado.");
        });
        User user = new User();
        user.setCompany(company);
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        User saved = userRepository.save(user);
        auditService.record(company, currentUserService.currentUser(), "USER_CREATED", "User", saved.getId(), null, saved.getEmail(), null);
        return mapper.user(saved);
    }

    @Transactional
    public UserResponse setUserActive(Long companyId, Long userId, boolean active) {
        Company company = findCompany(companyId);
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario nao encontrado."));
        boolean old = user.isActive();
        user.setActive(active);
        user.setUpdatedAt(Instant.now());
        auditService.record(company, currentUserService.currentUser(), "USER_STATUS_CHANGED", "User", userId,
            String.valueOf(old), String.valueOf(active), null);
        return mapper.user(user);
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> plans() {
        return planRepository.findAll().stream().map(mapper::plan).toList();
    }

    @Transactional
    public PlanResponse updatePlan(Long id, PlanUpdateRequest request) {
        Plan plan = planRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plano nao encontrado."));
        if (request.price() != null) plan.setPrice(request.price());
        if (request.maxUsers() != null) plan.setMaxUsers(request.maxUsers());
        if (request.maxProducts() != null) plan.setMaxProducts(request.maxProducts());
        if (request.hasReports() != null) plan.setHasReports(request.hasReports());
        if (request.hasAi() != null) plan.setHasAi(request.hasAi());
        if (request.hasWhatsapp() != null) plan.setHasWhatsapp(request.hasWhatsapp());
        if (request.hasBackup() != null) plan.setHasBackup(request.hasBackup());
        if (request.active() != null) plan.setActive(request.active());
        auditService.record(null, currentUserService.currentUser(), "PLAN_UPDATED", "Plan", id, null, plan.getName(), null);
        return mapper.plan(plan);
    }

    @Transactional(readOnly = true)
    public List<HealthResponse> health() {
        List<HealthResponse> logs = healthLogRepository.findTop20ByOrderByCreatedAtDesc().stream()
            .map(log -> new HealthResponse(log.getStatus(), log.getComponent(), log.getMessage(), log.getCreatedAt()))
            .toList();
        if (!logs.isEmpty()) {
            return logs;
        }
        return List.of(new HealthResponse("ONLINE", "API", "Backend respondendo normalmente.", Instant.now()));
    }

    private void applyCompany(Company company, CompanyRequest request) {
        Plan plan = planRepository.findById(request.planId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plano nao encontrado."));
        company.setName(request.name());
        company.setDocument(request.document());
        company.setEmail(request.email());
        company.setPhone(request.phone());
        company.setPlan(plan);
        company.setStatus(request.status() == null ? CompanyStatus.TRIAL : request.status());
        company.setTrialEndsAt(request.trialEndsAt());
        company.setSubscriptionEndsAt(request.subscriptionEndsAt());
    }

    private Company findCompany(Long id) {
        return companyRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Empresa nao encontrada."));
    }
}

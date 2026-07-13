package br.com.flowstock.web.dto;

import br.com.flowstock.domain.enums.CompanyStatus;
import br.com.flowstock.domain.enums.ProductionStatus;
import br.com.flowstock.domain.enums.SaleStatus;
import br.com.flowstock.domain.enums.StockMovementType;
import br.com.flowstock.domain.enums.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class CommonDtos {
    private CommonDtos() {
    }

    public record PlanResponse(Long id, String name, BigDecimal price, Integer maxUsers, Integer maxProducts,
                               boolean hasReports, boolean hasAi, boolean hasWhatsapp, boolean hasBackup, boolean active) {
    }

    public record PlanUpdateRequest(BigDecimal price, Integer maxUsers, Integer maxProducts, Boolean hasReports,
                                    Boolean hasAi, Boolean hasWhatsapp, Boolean hasBackup, Boolean active) {
    }

    public record CompanyRequest(@NotBlank String name, String document, @Email @NotBlank String email, String phone,
                                 @NotNull Long planId, CompanyStatus status, LocalDate trialEndsAt,
                                 LocalDate subscriptionEndsAt) {
    }

    public record CompanyOnboardingRequest(@NotBlank String name, String document, @Email @NotBlank String email,
                                           String phone, @NotNull Long planId, @NotNull CompanyStatus status,
                                           LocalDate trialEndsAt, LocalDate subscriptionEndsAt,
                                           @NotBlank String adminName, @Email @NotBlank String adminEmail,
                                           @NotBlank String adminPassword) {
    }

    public record CompanyResponse(Long id, String name, String document, String email, String phone,
                                  CompanyStatus status, LocalDate trialEndsAt, LocalDate subscriptionEndsAt,
                                  PlanResponse plan, Instant createdAt, Instant updatedAt,
                                  long usersCount, Instant lastAccessAt) {
    }

    public record UserCreateRequest(@NotBlank String name, @Email @NotBlank String email, @NotBlank String password,
                                    @NotNull UserRole role) {
    }

    public record UserResponse(Long id, String name, String email, UserRole role, Long companyId, boolean active,
                               Instant createdAt, Instant lastLoginAt) {
    }

    public record ProductRequest(@NotBlank(message = "Informe o nome do produto.") String name, String description, String sku, String category, String unit,
                                 @NotNull(message = "Informe o preco de custo.") @DecimalMin(value = "0.00", message = "O preco de custo nao pode ser negativo.") BigDecimal costPrice,
                                 @NotNull(message = "Informe o preco de venda.") @DecimalMin(value = "0.00", message = "O preco de venda nao pode ser negativo.") BigDecimal salePrice,
                                 @NotNull(message = "Informe o estoque minimo.") @DecimalMin(value = "0.000", message = "O estoque minimo nao pode ser menor que zero.") BigDecimal minStock,
                                 @DecimalMin(value = "0.000", message = "O estoque atual nao pode ser menor que zero.") BigDecimal currentStock,
                                 Boolean active) {
    }

    public record ProductResponse(Long id, String name, String description, String sku, String category, String unit,
                                  BigDecimal costPrice, BigDecimal salePrice, BigDecimal minStock,
                                  BigDecimal currentStock, boolean active, Instant createdAt, Instant updatedAt) {
    }

    public record StockMovementRequest(@NotNull Long productId, @NotNull StockMovementType type,
                                       @NotNull @DecimalMin("0.001") BigDecimal quantity, String reason) {
    }

    public record StockMovementResponse(Long id, Long productId, String productName, StockMovementType type,
                                        BigDecimal quantity, String reason, String createdBy, Instant createdAt) {
    }

    public record ProductionRequest(@NotNull Long productId, @NotNull @DecimalMin("0.001") BigDecimal quantityProduced,
                                    LocalDate productionDate, String notes) {
    }

    public record ProductionResponse(Long id, Long productId, String productName, BigDecimal quantityProduced,
                                     ProductionStatus status, LocalDate productionDate, String notes,
                                     String createdBy, Instant createdAt) {
    }

    public record CustomerRequest(@NotBlank String name, String phone, @Email String email, String document) {
    }

    public record CustomerResponse(Long id, String name, String phone, String email, String document,
                                   boolean active, Instant createdAt, Instant updatedAt) {
    }

    public record SaleItemRequest(@NotNull Long productId, @NotNull @DecimalMin("0.001") BigDecimal quantity,
                                  @DecimalMin("0.00") BigDecimal unitPrice) {
    }

    public record SaleRequest(Long customerId, @NotBlank String paymentMethod, SaleStatus status,
                              @NotEmpty List<@Valid SaleItemRequest> items) {
    }

    public record SaleItemResponse(Long productId, String productName, BigDecimal quantity, BigDecimal unitPrice,
                                   BigDecimal totalPrice) {
    }

    public record SaleResponse(Long id, Long customerId, String customerName, BigDecimal totalAmount, String paymentMethod,
                               SaleStatus status, String createdBy, Instant createdAt, List<SaleItemResponse> items) {
    }

    public record AuditLogResponse(Long id, Long companyId, String actor, String action, String entityName,
                                   String entityId, String oldValue, String newValue, String ipAddress, Instant createdAt) {
    }

    public record SuperAdminDashboardResponse(long activeCompanies, long trialCompanies, long blockedCompanies,
                                              BigDecimal expectedMonthlyRevenue, long overduePayments,
                                              long recentErrors, String apiStatus) {
    }

    public record CompanyDashboardResponse(long todaySalesCount, BigDecimal todayRevenue, long productsCount,
                                           long lowStockProductsCount, long recentProductionsCount,
                                           List<StockMovementResponse> recentStockMovements) {
    }

    public record TopProductResponse(String productName, BigDecimal quantity) {
    }

    public record HealthResponse(String status, String component, String message, Instant checkedAt) {
    }

    public record ReportSummaryResponse(CompanyDashboardResponse dashboard, List<ProductResponse> lowStockProducts,
                                        List<TopProductResponse> topProducts,
                                        List<ProductionResponse> recentProductions,
                                        List<StockMovementResponse> recentStockMovements) {
    }
}

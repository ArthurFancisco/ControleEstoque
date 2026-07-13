package br.com.flowstock.service;

import br.com.flowstock.domain.entity.Company;
import br.com.flowstock.domain.entity.Customer;
import br.com.flowstock.domain.entity.Plan;
import br.com.flowstock.domain.entity.Product;
import br.com.flowstock.domain.entity.ProductionBatch;
import br.com.flowstock.domain.entity.Sale;
import br.com.flowstock.domain.entity.SaleItem;
import br.com.flowstock.domain.entity.StockMovement;
import br.com.flowstock.domain.entity.User;
import br.com.flowstock.repository.UserRepository;
import br.com.flowstock.web.dto.CommonDtos.CompanyResponse;
import br.com.flowstock.web.dto.CommonDtos.CustomerResponse;
import br.com.flowstock.web.dto.CommonDtos.PlanResponse;
import br.com.flowstock.web.dto.CommonDtos.ProductResponse;
import br.com.flowstock.web.dto.CommonDtos.ProductionResponse;
import br.com.flowstock.web.dto.CommonDtos.SaleItemResponse;
import br.com.flowstock.web.dto.CommonDtos.SaleResponse;
import br.com.flowstock.web.dto.CommonDtos.StockMovementResponse;
import br.com.flowstock.web.dto.CommonDtos.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {
    private final UserRepository userRepository;

    public DtoMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PlanResponse plan(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getName(), plan.getPrice(), plan.getMaxUsers(), plan.getMaxProducts(),
            plan.isHasReports(), plan.isHasAi(), plan.isHasWhatsapp(), plan.isHasBackup(), plan.isActive());
    }

    public CompanyResponse company(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getDocument(), company.getEmail(), company.getPhone(),
            company.getStatus(), company.getTrialEndsAt(), company.getSubscriptionEndsAt(), plan(company.getPlan()),
            company.getCreatedAt(), company.getUpdatedAt(), userRepository.countByCompanyId(company.getId()),
            userRepository.findLastLoginByCompanyId(company.getId()));
    }

    public UserResponse user(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
            user.getCompany() == null ? null : user.getCompany().getId(), user.isActive(),
            user.getCreatedAt(), user.getLastLoginAt());
    }

    public ProductResponse product(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getSku(),
            product.getCategory(), product.getUnit(), product.getCostPrice(), product.getSalePrice(), product.getMinStock(),
            product.getCurrentStock(), product.isActive(), product.getCreatedAt(), product.getUpdatedAt());
    }

    public CustomerResponse customer(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(),
            customer.getDocument(), customer.isActive(), customer.getCreatedAt(), customer.getUpdatedAt());
    }

    public StockMovementResponse stockMovement(StockMovement movement) {
        return new StockMovementResponse(movement.getId(), movement.getProduct().getId(), movement.getProduct().getName(),
            movement.getType(), movement.getQuantity(), movement.getReason(),
            movement.getCreatedBy() == null ? "system" : movement.getCreatedBy().getName(), movement.getCreatedAt());
    }

    public ProductionResponse production(ProductionBatch batch) {
        return new ProductionResponse(batch.getId(), batch.getProduct().getId(), batch.getProduct().getName(),
            batch.getQuantityProduced(), batch.getStatus(), batch.getProductionDate(), batch.getNotes(),
            batch.getCreatedBy() == null ? "system" : batch.getCreatedBy().getName(), batch.getCreatedAt());
    }

    public SaleResponse sale(Sale sale) {
        return new SaleResponse(sale.getId(), sale.getCustomer() == null ? null : sale.getCustomer().getId(),
            sale.getCustomer() == null ? null : sale.getCustomer().getName(), sale.getTotalAmount(), sale.getPaymentMethod(),
            sale.getStatus(), sale.getCreatedBy() == null ? "system" : sale.getCreatedBy().getName(), sale.getCreatedAt(),
            sale.getItems().stream().map(this::saleItem).toList());
    }

    public SaleItemResponse saleItem(SaleItem item) {
        return new SaleItemResponse(item.getProduct().getId(), item.getProduct().getName(), item.getQuantity(),
            item.getUnitPrice(), item.getTotalPrice());
    }
}

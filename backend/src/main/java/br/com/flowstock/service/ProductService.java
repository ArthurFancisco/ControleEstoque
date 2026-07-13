package br.com.flowstock.service;

import br.com.flowstock.domain.entity.Company;
import br.com.flowstock.domain.entity.Product;
import br.com.flowstock.repository.CompanyRepository;
import br.com.flowstock.repository.ProductRepository;
import br.com.flowstock.web.dto.CommonDtos.ProductRequest;
import br.com.flowstock.web.dto.CommonDtos.ProductResponse;
import br.com.flowstock.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final DtoMapper mapper;

    public ProductService(ProductRepository productRepository, CompanyRepository companyRepository,
                          CurrentUserService currentUserService, AuditService auditService, DtoMapper mapper) {
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return productRepository.findByCompanyIdOrderByName(currentUserService.requireCompanyId()).stream().map(mapper::product).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Long companyId = currentUserService.requireCompanyId();
        validateSku(companyId, null, request.sku());
        Company company = companyRepository.getReferenceById(companyId);
        Product product = new Product();
        product.setCompany(company);
        apply(product, request);
        product.setCurrentStock(request.currentStock() == null ? BigDecimal.ZERO : request.currentStock());
        Product saved = productRepository.save(product);
        auditService.record(company, currentUserService.currentUser(), "PRODUCT_CREATED", "Product", saved.getId(), null, saved.getName(), null);
        return mapper.product(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOwned(id);
        validateSku(product.getCompany().getId(), id, request.sku());
        String old = product.getName();
        apply(product, request);
        product.setUpdatedAt(Instant.now());
        auditService.record(product.getCompany(), currentUserService.currentUser(), "PRODUCT_UPDATED", "Product", id, old, product.getName(), null);
        return mapper.product(product);
    }

    @Transactional
    public ProductResponse setActive(Long id, boolean active) {
        Product product = findOwned(id);
        boolean old = product.isActive();
        product.setActive(active);
        product.setUpdatedAt(Instant.now());
        auditService.record(product.getCompany(), currentUserService.currentUser(), "PRODUCT_STATUS_CHANGED", "Product", id, String.valueOf(old), String.valueOf(active), null);
        return mapper.product(product);
    }

    @Transactional
    public ProductResponse toggleActive(Long id) {
        Product product = findOwned(id);
        return setActive(id, !product.isActive());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> lowStock() {
        return productRepository.findByCompanyIdOrderByName(currentUserService.requireCompanyId()).stream()
            .filter(product -> product.getCurrentStock().compareTo(product.getMinStock()) <= 0)
            .map(mapper::product)
            .toList();
    }

    public Product findOwned(Long id) {
        Long companyId = currentUserService.requireCompanyId();
        return productRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produto nao encontrado."));
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku() == null || request.sku().isBlank() ? null : request.sku().trim());
        product.setCategory(request.category());
        product.setUnit(request.unit() == null || request.unit().isBlank() ? "UN" : request.unit());
        product.setCostPrice(request.costPrice());
        product.setSalePrice(request.salePrice());
        product.setMinStock(request.minStock());
        if (request.currentStock() != null) {
            product.setCurrentStock(request.currentStock());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
    }

    private void validateSku(Long companyId, Long productId, String sku) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        boolean exists = productId == null
            ? productRepository.existsByCompanyIdAndSkuIgnoreCase(companyId, sku)
            : productRepository.existsByCompanyIdAndSkuIgnoreCaseAndIdNot(companyId, sku, productId);
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "SKU ja existente nesta empresa.");
        }
    }
}

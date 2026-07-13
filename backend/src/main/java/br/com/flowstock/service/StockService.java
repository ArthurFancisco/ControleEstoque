package br.com.flowstock.service;

import br.com.flowstock.domain.entity.Product;
import br.com.flowstock.domain.entity.StockMovement;
import br.com.flowstock.domain.enums.StockMovementType;
import br.com.flowstock.repository.StockMovementRepository;
import br.com.flowstock.web.dto.CommonDtos.StockMovementRequest;
import br.com.flowstock.web.dto.CommonDtos.StockMovementResponse;
import br.com.flowstock.web.dto.CommonDtos.ProductResponse;
import br.com.flowstock.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class StockService {
    private final StockMovementRepository stockMovementRepository;
    private final ProductService productService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final DtoMapper mapper;

    public StockService(StockMovementRepository stockMovementRepository, ProductService productService,
                        CurrentUserService currentUserService, AuditService auditService, DtoMapper mapper) {
        this.stockMovementRepository = stockMovementRepository;
        this.productService = productService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> stock() {
        return productService.list();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> history() {
        return stockMovementRepository.findTop100ByCompanyIdOrderByCreatedAtDesc(currentUserService.requireCompanyId())
            .stream().map(mapper::stockMovement).toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> recent(int limit) {
        return history().stream().limit(limit).toList();
    }

    @Transactional
    public StockMovementResponse move(StockMovementRequest request) {
        Product product = productService.findOwned(request.productId());
        return mapper.stockMovement(applyMovement(product, request.type(), request.quantity(), request.reason()));
    }

    @Transactional
    public StockMovement applyMovement(Product product, StockMovementType type, BigDecimal quantity, String reason) {
        BigDecimal oldStock = product.getCurrentStock();
        BigDecimal newStock = switch (type) {
            case IN, PRODUCTION -> oldStock.add(quantity);
            case OUT, SALE -> oldStock.subtract(quantity);
            case ADJUSTMENT -> quantity;
        };
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estoque insuficiente para a operacao.");
        }
        product.setCurrentStock(newStock);
        product.setUpdatedAt(Instant.now());

        StockMovement movement = new StockMovement();
        movement.setCompany(product.getCompany());
        movement.setProduct(product);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setReason(reason);
        movement.setCreatedBy(currentUserService.currentUser());
        StockMovement saved = stockMovementRepository.save(movement);
        auditService.record(product.getCompany(), currentUserService.currentUser(), "STOCK_MOVEMENT", "Product",
            product.getId(), oldStock.toPlainString(), newStock.toPlainString(), null);
        return saved;
    }
}

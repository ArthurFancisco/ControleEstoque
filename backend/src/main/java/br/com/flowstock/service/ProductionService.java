package br.com.flowstock.service;

import br.com.flowstock.domain.entity.Product;
import br.com.flowstock.domain.entity.ProductionBatch;
import br.com.flowstock.domain.enums.ProductionStatus;
import br.com.flowstock.domain.enums.StockMovementType;
import br.com.flowstock.repository.ProductionBatchRepository;
import br.com.flowstock.web.dto.CommonDtos.ProductionRequest;
import br.com.flowstock.web.dto.CommonDtos.ProductionResponse;
import br.com.flowstock.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProductionService {
    private final ProductionBatchRepository productionBatchRepository;
    private final ProductService productService;
    private final StockService stockService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final DtoMapper mapper;

    public ProductionService(ProductionBatchRepository productionBatchRepository, ProductService productService,
                             StockService stockService, CurrentUserService currentUserService,
                             AuditService auditService, DtoMapper mapper) {
        this.productionBatchRepository = productionBatchRepository;
        this.productService = productService;
        this.stockService = stockService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProductionResponse> list() {
        return productionBatchRepository.findByCompanyIdOrderByCreatedAtDesc(currentUserService.requireCompanyId())
            .stream().map(mapper::production).toList();
    }

    @Transactional
    public ProductionResponse create(ProductionRequest request) {
        Product product = productService.findOwned(request.productId());
        ProductionBatch batch = new ProductionBatch();
        batch.setCompany(product.getCompany());
        batch.setProduct(product);
        batch.setQuantityProduced(request.quantityProduced());
        batch.setProductionDate(request.productionDate() == null ? LocalDate.now() : request.productionDate());
        batch.setNotes(request.notes());
        batch.setCreatedBy(currentUserService.currentUser());
        ProductionBatch saved = productionBatchRepository.save(batch);
        auditService.record(product.getCompany(), currentUserService.currentUser(), "PRODUCTION_CREATED", "ProductionBatch", saved.getId(), null, product.getName(), null);
        return mapper.production(saved);
    }

    @Transactional
    public ProductionResponse finish(Long id) {
        ProductionBatch batch = findOwned(id);
        if (batch.getStatus() == ProductionStatus.FINISHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Producao ja finalizada.");
        }
        if (batch.getStatus() == ProductionStatus.CANCELED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Producao cancelada nao pode ser finalizada.");
        }
        batch.setStatus(ProductionStatus.FINISHED);
        stockService.applyMovement(batch.getProduct(), StockMovementType.PRODUCTION, batch.getQuantityProduced(), "Finalizacao de producao " + batch.getId());
        auditService.record(batch.getCompany(), currentUserService.currentUser(), "PRODUCTION_FINISHED", "ProductionBatch", id, null, batch.getQuantityProduced().toPlainString(), null);
        return mapper.production(batch);
    }

    @Transactional
    public ProductionResponse cancel(Long id) {
        ProductionBatch batch = findOwned(id);
        if (batch.getStatus() == ProductionStatus.FINISHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Producao finalizada nao pode ser cancelada.");
        }
        batch.setStatus(ProductionStatus.CANCELED);
        auditService.record(batch.getCompany(), currentUserService.currentUser(), "PRODUCTION_CANCELED", "ProductionBatch", id, null, null, null);
        return mapper.production(batch);
    }

    private ProductionBatch findOwned(Long id) {
        return productionBatchRepository.findByIdAndCompanyId(id, currentUserService.requireCompanyId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producao nao encontrada."));
    }
}

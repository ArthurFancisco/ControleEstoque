package br.com.flowstock.service;

import br.com.flowstock.domain.enums.ProductionStatus;
import br.com.flowstock.domain.enums.SaleStatus;
import br.com.flowstock.repository.ProductRepository;
import br.com.flowstock.repository.ProductionBatchRepository;
import br.com.flowstock.repository.SaleItemRepository;
import br.com.flowstock.repository.SaleRepository;
import br.com.flowstock.web.dto.CommonDtos.CompanyDashboardResponse;
import br.com.flowstock.web.dto.CommonDtos.ProductResponse;
import br.com.flowstock.web.dto.CommonDtos.ProductionResponse;
import br.com.flowstock.web.dto.CommonDtos.ReportSummaryResponse;
import br.com.flowstock.web.dto.CommonDtos.TopProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ReportService {
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductionBatchRepository productionBatchRepository;
    private final CurrentUserService currentUserService;
    private final StockService stockService;
    private final DtoMapper mapper;

    public ReportService(ProductRepository productRepository, SaleRepository saleRepository,
                         SaleItemRepository saleItemRepository, ProductionBatchRepository productionBatchRepository,
                         CurrentUserService currentUserService, StockService stockService, DtoMapper mapper) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productionBatchRepository = productionBatchRepository;
        this.currentUserService = currentUserService;
        this.stockService = stockService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CompanyDashboardResponse dashboard() {
        Long companyId = currentUserService.requireCompanyId();
        var period = todayPeriod();
        return new CompanyDashboardResponse(
            saleRepository.countByCompanyIdAndStatusAndCreatedAtBetween(companyId, SaleStatus.PAID, period.start(), period.end()),
            saleRepository.sumTotalByCompanyAndStatusAndPeriod(companyId, SaleStatus.PAID, period.start(), period.end()),
            productRepository.countByCompanyId(companyId),
            productRepository.findByCompanyIdOrderByName(companyId).stream()
                .filter(p -> p.getCurrentStock().compareTo(p.getMinStock()) <= 0)
                .count(),
            productionBatchRepository.countByCompanyIdAndStatus(companyId, ProductionStatus.FINISHED),
            stockService.recent(6)
        );
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> lowStock() {
        Long companyId = currentUserService.requireCompanyId();
        return productRepository.findByCompanyIdOrderByName(companyId).stream()
            .filter(product -> product.getCurrentStock().compareTo(product.getMinStock()) <= 0)
            .map(mapper::product)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts() {
        return saleItemRepository.topProducts(currentUserService.requireCompanyId()).stream()
            .map(row -> new TopProductResponse(row.getProductName(), row.getQuantity()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionResponse> recentProductions() {
        return productionBatchRepository.findTop10ByCompanyIdOrderByCreatedAtDesc(currentUserService.requireCompanyId())
            .stream().map(mapper::production).toList();
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse summary() {
        return new ReportSummaryResponse(dashboard(), lowStock(), topProducts(), recentProductions(), stockService.recent(10));
    }

    private Period todayPeriod() {
        ZoneId zone = ZoneId.systemDefault();
        return new Period(LocalDate.now().atStartOfDay(zone).toInstant(), LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant());
    }

    private record Period(java.time.Instant start, java.time.Instant end) {
    }
}

package br.com.flowstock.web.controller;

import br.com.flowstock.service.ReportService;
import br.com.flowstock.web.dto.CommonDtos.CompanyDashboardResponse;
import br.com.flowstock.web.dto.CommonDtos.ProductResponse;
import br.com.flowstock.web.dto.CommonDtos.ProductionResponse;
import br.com.flowstock.web.dto.CommonDtos.ReportSummaryResponse;
import br.com.flowstock.web.dto.CommonDtos.TopProductResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/reports", "/api/app/reports"})
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','EMPLOYEE')")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public CompanyDashboardResponse dashboard() {
        return reportService.dashboard();
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock() {
        return reportService.lowStock();
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> topProducts() {
        return reportService.topProducts();
    }

    @GetMapping("/recent-productions")
    public List<ProductionResponse> recentProductions() {
        return reportService.recentProductions();
    }

    @GetMapping("/summary")
    public ReportSummaryResponse summary() {
        return reportService.summary();
    }
}

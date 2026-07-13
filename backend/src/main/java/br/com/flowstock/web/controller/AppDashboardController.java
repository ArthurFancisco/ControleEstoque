package br.com.flowstock.web.controller;

import br.com.flowstock.service.ReportService;
import br.com.flowstock.web.dto.CommonDtos.CompanyDashboardResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/dashboard")
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','EMPLOYEE')")
public class AppDashboardController {
    private final ReportService reportService;

    public AppDashboardController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public CompanyDashboardResponse dashboard() {
        return reportService.dashboard();
    }
}

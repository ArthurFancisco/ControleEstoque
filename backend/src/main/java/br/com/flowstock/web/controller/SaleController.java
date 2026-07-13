package br.com.flowstock.web.controller;

import br.com.flowstock.service.SaleService;
import br.com.flowstock.web.dto.CommonDtos.SaleRequest;
import br.com.flowstock.web.dto.CommonDtos.SaleResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/sales", "/api/app/sales"})
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','EMPLOYEE')")
public class SaleController {
    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public List<SaleResponse> list() {
        return saleService.list();
    }

    @PostMapping
    public SaleResponse create(@Valid @RequestBody SaleRequest request) {
        return saleService.create(request);
    }

    @PatchMapping("/{id}/pay")
    public SaleResponse pay(@PathVariable Long id) {
        return saleService.pay(id);
    }

    @PatchMapping("/{id}/cancel")
    public SaleResponse cancel(@PathVariable Long id) {
        return saleService.cancel(id);
    }
}

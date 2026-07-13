package br.com.flowstock.web.controller;

import br.com.flowstock.service.StockService;
import br.com.flowstock.web.dto.CommonDtos.ProductResponse;
import br.com.flowstock.web.dto.CommonDtos.StockMovementRequest;
import br.com.flowstock.web.dto.CommonDtos.StockMovementResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/stock", "/api/app/stock"})
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','EMPLOYEE')")
public class StockController {
    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public List<ProductResponse> stock() {
        return stockService.stock();
    }

    @GetMapping("/movements")
    public List<StockMovementResponse> history() {
        return stockService.history();
    }

    @PostMapping("/movements")
    public StockMovementResponse move(@Valid @RequestBody StockMovementRequest request) {
        return stockService.move(request);
    }
}

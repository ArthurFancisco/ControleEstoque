package br.com.flowstock.web.controller;

import br.com.flowstock.service.ProductionService;
import br.com.flowstock.web.dto.CommonDtos.ProductionRequest;
import br.com.flowstock.web.dto.CommonDtos.ProductionResponse;
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
@RequestMapping({"/production", "/api/app/production"})
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','EMPLOYEE')")
public class ProductionController {
    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping
    public List<ProductionResponse> list() {
        return productionService.list();
    }

    @PostMapping
    public ProductionResponse create(@Valid @RequestBody ProductionRequest request) {
        return productionService.create(request);
    }

    @PatchMapping("/{id}/finish")
    public ProductionResponse finish(@PathVariable Long id) {
        return productionService.finish(id);
    }

    @PatchMapping("/{id}/cancel")
    public ProductionResponse cancel(@PathVariable Long id) {
        return productionService.cancel(id);
    }
}

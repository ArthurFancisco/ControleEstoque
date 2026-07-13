package br.com.flowstock.web.controller;

import br.com.flowstock.service.ProductService;
import br.com.flowstock.web.dto.CommonDtos.ProductRequest;
import br.com.flowstock.web.dto.CommonDtos.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/products", "/api/app/products"})
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','EMPLOYEE')")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ProductResponse active(@PathVariable Long id, @RequestParam boolean active) {
        return productService.setActive(id, active);
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ProductResponse toggleActive(@PathVariable Long id) {
        return productService.toggleActive(id);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock() {
        return productService.lowStock();
    }
}

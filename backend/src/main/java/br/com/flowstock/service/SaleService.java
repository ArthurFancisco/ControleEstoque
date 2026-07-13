package br.com.flowstock.service;

import br.com.flowstock.domain.entity.Customer;
import br.com.flowstock.domain.entity.Product;
import br.com.flowstock.domain.entity.Sale;
import br.com.flowstock.domain.entity.SaleItem;
import br.com.flowstock.domain.enums.SaleStatus;
import br.com.flowstock.domain.enums.StockMovementType;
import br.com.flowstock.repository.SaleRepository;
import br.com.flowstock.web.dto.CommonDtos.SaleItemRequest;
import br.com.flowstock.web.dto.CommonDtos.SaleRequest;
import br.com.flowstock.web.dto.CommonDtos.SaleResponse;
import br.com.flowstock.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SaleService {
    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final CustomerService customerService;
    private final StockService stockService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final DtoMapper mapper;

    public SaleService(SaleRepository saleRepository, ProductService productService, CustomerService customerService,
                       StockService stockService, CurrentUserService currentUserService, AuditService auditService,
                       DtoMapper mapper) {
        this.saleRepository = saleRepository;
        this.productService = productService;
        this.customerService = customerService;
        this.stockService = stockService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> list() {
        return saleRepository.findByCompanyIdOrderByCreatedAtDesc(currentUserService.requireCompanyId()).stream().map(mapper::sale).toList();
    }

    @Transactional
    public SaleResponse create(SaleRequest request) {
        Sale sale = new Sale();
        Customer customer = request.customerId() == null ? null : customerService.findOwned(request.customerId());
        sale.setCustomer(customer);
        sale.setPaymentMethod(request.paymentMethod());
        SaleStatus requestedStatus = request.status() == null ? SaleStatus.PAID : request.status();
        if (requestedStatus == SaleStatus.CANCELED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Venda nova nao pode iniciar cancelada.");
        }
        sale.setStatus(requestedStatus);
        sale.setCreatedBy(currentUserService.currentUser());

        BigDecimal total = BigDecimal.ZERO;
        for (SaleItemRequest itemRequest : request.items()) {
            Product product = productService.findOwned(itemRequest.productId());
            if (product.getCurrentStock().compareTo(itemRequest.quantity()) < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Estoque insuficiente para vender " + product.getName() + ".");
            }
            if (sale.getCompany() == null) {
                sale.setCompany(product.getCompany());
            }
            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            BigDecimal unitPrice = itemRequest.unitPrice() == null ? product.getSalePrice() : itemRequest.unitPrice();
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(unitPrice.multiply(itemRequest.quantity()));
            total = total.add(item.getTotalPrice());
            sale.getItems().add(item);
        }
        sale.setTotalAmount(total);
        Sale saved = saleRepository.save(sale);
        if (saved.getStatus() == SaleStatus.PAID) {
            saved.getItems().forEach(item -> stockService.applyMovement(item.getProduct(), StockMovementType.SALE, item.getQuantity(), "Venda paga " + saved.getId()));
            auditService.record(saved.getCompany(), currentUserService.currentUser(), "SALE_PAID", "Sale", saved.getId(), null, total.toPlainString(), null);
        }
        auditService.record(saved.getCompany(), currentUserService.currentUser(), "SALE_CREATED", "Sale", saved.getId(), null, total.toPlainString(), null);
        return mapper.sale(saved);
    }

    @Transactional
    public SaleResponse pay(Long id) {
        Sale sale = saleRepository.findByIdAndCompanyId(id, currentUserService.requireCompanyId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Venda nao encontrada."));
        if (sale.getStatus() == SaleStatus.CANCELED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Venda cancelada nao pode ser paga.");
        }
        if (sale.getStatus() == SaleStatus.PAID) {
            return mapper.sale(sale);
        }
        sale.setStatus(SaleStatus.PAID);
        sale.getItems().forEach(item -> stockService.applyMovement(item.getProduct(), StockMovementType.SALE, item.getQuantity(), "Pagamento da venda " + sale.getId()));
        auditService.record(sale.getCompany(), currentUserService.currentUser(), "SALE_PAID", "Sale", id, null, sale.getTotalAmount().toPlainString(), null);
        return mapper.sale(sale);
    }

    @Transactional
    public SaleResponse cancel(Long id) {
        Sale sale = saleRepository.findByIdAndCompanyId(id, currentUserService.requireCompanyId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Venda nao encontrada."));
        if (sale.getStatus() == SaleStatus.CANCELED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Venda ja cancelada.");
        }
        SaleStatus oldStatus = sale.getStatus();
        sale.setStatus(SaleStatus.CANCELED);
        if (oldStatus == SaleStatus.PAID) {
            sale.getItems().forEach(item -> stockService.applyMovement(item.getProduct(), StockMovementType.IN, item.getQuantity(), "Cancelamento da venda " + sale.getId()));
        }
        auditService.record(sale.getCompany(), currentUserService.currentUser(), "SALE_CANCELED", "Sale", id, null, null, null);
        return mapper.sale(sale);
    }
}

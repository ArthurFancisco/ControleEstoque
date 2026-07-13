package br.com.flowstock.service;

import br.com.flowstock.domain.entity.Company;
import br.com.flowstock.domain.entity.Customer;
import br.com.flowstock.repository.CompanyRepository;
import br.com.flowstock.repository.CustomerRepository;
import br.com.flowstock.web.dto.CommonDtos.CustomerRequest;
import br.com.flowstock.web.dto.CommonDtos.CustomerResponse;
import br.com.flowstock.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final DtoMapper mapper;

    public CustomerService(CustomerRepository customerRepository, CompanyRepository companyRepository,
                           CurrentUserService currentUserService, AuditService auditService, DtoMapper mapper) {
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list() {
        return customerRepository.findByCompanyIdOrderByName(currentUserService.requireCompanyId()).stream().map(mapper::customer).toList();
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Company company = companyRepository.getReferenceById(currentUserService.requireCompanyId());
        Customer customer = new Customer();
        customer.setCompany(company);
        apply(customer, request);
        customer.setActive(true);
        Customer saved = customerRepository.save(customer);
        auditService.record(company, currentUserService.currentUser(), "CUSTOMER_CREATED", "Customer", saved.getId(), null, saved.getName(), null);
        return mapper.customer(saved);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findOwned(id);
        apply(customer, request);
        customer.setUpdatedAt(Instant.now());
        auditService.record(customer.getCompany(), currentUserService.currentUser(), "CUSTOMER_UPDATED", "Customer", id, null, customer.getName(), null);
        return mapper.customer(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findOwned(id);
        boolean oldValue = customer.isActive();
        customer.setActive(false);
        customer.setUpdatedAt(Instant.now());
        auditService.record(customer.getCompany(), currentUserService.currentUser(), "CUSTOMER_DEACTIVATED", "Customer", id, String.valueOf(oldValue), "false", null);
    }

    public Customer findOwned(Long id) {
        return customerRepository.findByIdAndCompanyId(id, currentUserService.requireCompanyId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cliente nao encontrado."));
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setDocument(request.document());
    }
}

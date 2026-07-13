package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByCompanyIdOrderByName(Long companyId);
    Optional<Customer> findByIdAndCompanyId(Long id, Long companyId);
}

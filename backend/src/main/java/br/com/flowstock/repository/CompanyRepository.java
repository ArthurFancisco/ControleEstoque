package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.Company;
import br.com.flowstock.domain.enums.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    long countByStatus(CompanyStatus status);
}

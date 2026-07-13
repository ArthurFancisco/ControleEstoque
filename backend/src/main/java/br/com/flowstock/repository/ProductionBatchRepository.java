package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.ProductionBatch;
import br.com.flowstock.domain.enums.ProductionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, Long> {
    List<ProductionBatch> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<ProductionBatch> findTop10ByCompanyIdOrderByCreatedAtDesc(Long companyId);
    Optional<ProductionBatch> findByIdAndCompanyId(Long id, Long companyId);
    long countByCompanyIdAndStatus(Long companyId, ProductionStatus status);
}

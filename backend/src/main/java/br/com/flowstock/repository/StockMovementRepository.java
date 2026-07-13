package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findTop100ByCompanyIdOrderByCreatedAtDesc(Long companyId);
}

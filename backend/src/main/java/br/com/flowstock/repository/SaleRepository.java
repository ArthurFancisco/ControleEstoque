package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.Sale;
import br.com.flowstock.domain.enums.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    Optional<Sale> findByIdAndCompanyId(Long id, Long companyId);
    long countByCompanyIdAndStatusAndCreatedAtBetween(Long companyId, SaleStatus status, Instant start, Instant end);

    @Query("select coalesce(sum(s.totalAmount), 0) from Sale s where s.company.id = :companyId and s.status = :status and s.createdAt between :start and :end")
    BigDecimal sumTotalByCompanyAndStatusAndPeriod(@Param("companyId") Long companyId, @Param("status") SaleStatus status, @Param("start") Instant start, @Param("end") Instant end);
}

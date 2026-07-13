package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCompanyIdOrderByName(Long companyId);
    Optional<Product> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsByCompanyIdAndSkuIgnoreCase(Long companyId, String sku);
    boolean existsByCompanyIdAndSkuIgnoreCaseAndIdNot(Long companyId, String sku, Long id);
    long countByCompanyId(Long companyId);
    long countByCompanyIdAndCurrentStockLessThanEqual(Long companyId, BigDecimal stock);
    List<Product> findByCompanyIdAndCurrentStockLessThanEqualOrderByCurrentStockAsc(Long companyId, BigDecimal stock);
}

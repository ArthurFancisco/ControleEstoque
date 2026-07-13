package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    @Query(value = """
        select p.name as productName, coalesce(sum(si.quantity), 0) as quantity
        from sale_items si
        join products p on p.id = si.product_id
        join sales s on s.id = si.sale_id
        where s.company_id = :companyId
          and s.status = 'PAID'
        group by p.name
        order by sum(si.quantity) desc
        limit 10
    """, nativeQuery = true)
    List<TopProductProjection> topProducts(@Param("companyId") Long companyId);

    interface TopProductProjection {
        String getProductName();
        java.math.BigDecimal getQuantity();
    }
}

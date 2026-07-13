package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop200ByOrderByCreatedAtDesc();
    List<AuditLog> findTop100ByCompanyIdOrderByCreatedAtDesc(Long companyId);
}

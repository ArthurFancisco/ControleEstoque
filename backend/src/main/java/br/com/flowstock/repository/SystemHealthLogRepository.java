package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.SystemHealthLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemHealthLogRepository extends JpaRepository<SystemHealthLog, Long> {
    List<SystemHealthLog> findTop20ByOrderByCreatedAtDesc();
}

package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.SupportSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportSessionRepository extends JpaRepository<SupportSession, Long> {
}

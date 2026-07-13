package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByNameIgnoreCase(String name);
}

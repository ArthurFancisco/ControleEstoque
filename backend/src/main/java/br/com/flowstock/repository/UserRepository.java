package br.com.flowstock.repository;

import br.com.flowstock.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByIdAndCompanyId(Long id, Long companyId);
    List<User> findByCompanyIdOrderByName(Long companyId);
    long countByCompanyId(Long companyId);

    @Query("select max(u.lastLoginAt) from User u where u.company.id = :companyId")
    java.time.Instant findLastLoginByCompanyId(@Param("companyId") Long companyId);
}

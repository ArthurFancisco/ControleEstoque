package br.com.flowstock.service;

import br.com.flowstock.domain.entity.AuditLog;
import br.com.flowstock.domain.entity.Company;
import br.com.flowstock.domain.entity.User;
import br.com.flowstock.repository.AuditLogRepository;
import br.com.flowstock.web.dto.CommonDtos.AuditLogResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Company company, User actor, String action, String entityName, Object entityId,
                       String oldValue, String newValue, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setCompany(company);
        log.setActorUser(actor);
        log.setAction(action);
        log.setEntityName(entityName);
        log.setEntityId(entityId == null ? null : entityId.toString());
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> latestGlobal() {
        return auditLogRepository.findTop200ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    public List<AuditLogResponse> latestForCompany(Long companyId) {
        return auditLogRepository.findTop100ByCompanyIdOrderByCreatedAtDesc(companyId).stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getCompany() == null ? null : log.getCompany().getId(),
            log.getActorUser() == null ? "system" : log.getActorUser().getName(),
            log.getAction(),
            log.getEntityName(),
            log.getEntityId(),
            log.getOldValue(),
            log.getNewValue(),
            log.getIpAddress(),
            log.getCreatedAt()
        );
    }
}

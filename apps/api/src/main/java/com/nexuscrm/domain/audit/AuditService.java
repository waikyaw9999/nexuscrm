package com.nexuscrm.domain.audit;

import com.nexuscrm.tenant.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
class AuditLog extends TenantScopedEntity {
    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(columnDefinition = "text")
    private String details;

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

interface AuditLogRepository extends org.springframework.data.jpa.repository.JpaRepository<AuditLog, UUID> {}

@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(UUID tenantId, UUID userId, String action, String entityType, UUID entityId, String details) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        repository.save(log);
    }
}

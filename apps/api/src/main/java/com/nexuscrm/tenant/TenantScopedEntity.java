package com.nexuscrm.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@MappedSuperclass
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantScopedEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @PrePersist
    void assignTenant() {
        if (tenantId == null && TenantContext.tenantId() != null) {
            tenantId = TenantContext.tenantId();
        }
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
}

package com.nexuscrm.domain.membership;

import com.nexuscrm.tenant.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@Entity
@Table(name = "memberships")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Membership extends BaseEntity {
    public enum Role { OWNER, ADMIN, MEMBER, VIEWER }

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public Membership() {}

    public Membership(UUID id, UUID tenantId, UUID userId, Role role) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.role = role;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}

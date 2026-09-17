package com.nexuscrm.domain.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    List<Membership> findByUserId(UUID userId);
    List<Membership> findByTenantId(UUID tenantId);
    Optional<Membership> findByTenantIdAndUserId(UUID tenantId, UUID userId);
    Optional<Membership> findByIdAndTenantId(UUID id, UUID tenantId);
}

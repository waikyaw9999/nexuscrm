package com.nexuscrm.domain.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    List<Activity> findByTenantIdAndDeletedAtIsNullOrderByDueAtAsc(UUID tenantId);

    Optional<Activity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    long countByTenantIdAndDeletedAtIsNullAndCompletedAtIsNull(UUID tenantId);
}

package com.nexuscrm.domain.deal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DealRepository extends JpaRepository<Deal, UUID> {
    List<Deal> findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID tenantId);

    Optional<Deal> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    long countByTenantIdAndDeletedAtIsNullAndStage(UUID tenantId, Deal.Stage stage);

    @Query("""
            SELECT d FROM Deal d
            WHERE d.tenantId = :tenantId
              AND d.deletedAt IS NULL
              AND LOWER(COALESCE(d.title, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY d.updatedAt DESC
            """)
    List<Deal> searchByTitle(@Param("tenantId") UUID tenantId, @Param("q") String q);
}

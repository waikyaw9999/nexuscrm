package com.nexuscrm.domain.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Page<Company> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Company> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Query("""
            SELECT c FROM Company c
            WHERE c.tenantId = :tenantId
              AND c.deletedAt IS NULL
              AND LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Company> search(@Param("tenantId") UUID tenantId, @Param("q") String q, Pageable pageable);
}

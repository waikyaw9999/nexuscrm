package com.nexuscrm.domain.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
    Page<Contact> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Contact> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    @Query("""
            SELECT c FROM Contact c
            WHERE c.tenantId = :tenantId
              AND c.deletedAt IS NULL
              AND (
                    LOWER(CONCAT(COALESCE(c.firstName, ''), ' ', COALESCE(c.lastName, ''))) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Contact> search(@Param("tenantId") UUID tenantId, @Param("q") String q, Pageable pageable);
}

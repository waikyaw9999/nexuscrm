package com.nexuscrm.domain.mail;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MailboxRepository extends JpaRepository<Mailbox, UUID> {
    List<Mailbox> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    Optional<Mailbox> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}

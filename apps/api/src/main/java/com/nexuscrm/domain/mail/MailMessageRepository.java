package com.nexuscrm.domain.mail;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MailMessageRepository extends JpaRepository<MailMessage, UUID> {
    List<MailMessage> findByTenantIdAndMailboxIdAndFolderOrderBySentAtDesc(UUID tenantId, UUID mailboxId, String folder);

    Optional<MailMessage> findByIdAndTenantId(UUID id, UUID tenantId);

    List<MailMessage> findByMailboxId(UUID mailboxId);
}

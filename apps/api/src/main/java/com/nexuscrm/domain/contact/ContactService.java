package com.nexuscrm.domain.contact;

import com.nexuscrm.common.ApiException;
import com.nexuscrm.common.PageResponse;
import com.nexuscrm.domain.audit.AuditService;
import com.nexuscrm.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ContactService {
    private final ContactRepository contacts;
    private final AuditService auditService;

    public ContactService(ContactRepository contacts, AuditService auditService) {
        this.contacts = contacts;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<Contact> list(String q, int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("lastName", "firstName"));
        Page<Contact> result = (q == null || q.isBlank())
                ? contacts.findByTenantIdAndDeletedAtIsNull(tenantId, pageable)
                : contacts.search(tenantId, q.trim(), pageable);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public Contact get(UUID id) {
        return contacts.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Contact not found"));
    }

    @Transactional
    public Contact create(Contact body) {
        body.setId(UUID.randomUUID());
        body.setTenantId(TenantContext.requireTenantId());
        contacts.save(body);
        auditService.record(body.getTenantId(), TenantContext.userId(), "contact.created", "contact", body.getId(),
                body.getEmail());
        return body;
    }

    @Transactional
    public Contact update(UUID id, Contact incoming) {
        Contact existing = get(id);
        existing.setFirstName(incoming.getFirstName());
        existing.setLastName(incoming.getLastName());
        existing.setEmail(incoming.getEmail());
        existing.setPhone(incoming.getPhone());
        existing.setTitle(incoming.getTitle());
        existing.setTags(incoming.getTags());
        existing.setNotes(incoming.getNotes());
        existing.setCompanyId(incoming.getCompanyId());
        return existing;
    }

    @Transactional
    public void delete(UUID id) {
        Contact existing = get(id);
        existing.setDeletedAt(Instant.now());
    }
}

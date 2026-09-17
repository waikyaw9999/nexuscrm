package com.nexuscrm.domain.company;

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
public class CompanyService {
    private final CompanyRepository companies;
    private final AuditService auditService;

    public CompanyService(CompanyRepository companies, AuditService auditService) {
        this.companies = companies;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<Company> list(String q, int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("name"));
        Page<Company> result = (q == null || q.isBlank())
                ? companies.findByTenantIdAndDeletedAtIsNull(tenantId, pageable)
                : companies.search(tenantId, q.trim(), pageable);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public Company get(UUID id) {
        return companies.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Company not found"));
    }

    @Transactional
    public Company create(Company body) {
        body.setId(UUID.randomUUID());
        body.setTenantId(TenantContext.requireTenantId());
        companies.save(body);
        auditService.record(body.getTenantId(), TenantContext.userId(), "company.created", "company", body.getId(),
                body.getName());
        return body;
    }

    @Transactional
    public Company update(UUID id, Company incoming) {
        Company existing = get(id);
        existing.setName(incoming.getName());
        existing.setDomain(incoming.getDomain());
        existing.setIndustry(incoming.getIndustry());
        existing.setPhone(incoming.getPhone());
        existing.setWebsite(incoming.getWebsite());
        existing.setNotes(incoming.getNotes());
        return existing;
    }

    @Transactional
    public void delete(UUID id) {
        get(id).setDeletedAt(Instant.now());
    }
}

package com.nexuscrm.domain.deal;

import com.nexuscrm.common.ApiException;
import com.nexuscrm.domain.audit.AuditService;
import com.nexuscrm.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DealService {
    private final DealRepository deals;
    private final AuditService auditService;

    public DealService(DealRepository deals, AuditService auditService) {
        this.deals = deals;
        this.auditService = auditService;
    }

    /**
     * Unfiltered lists use tenant-scoped find. Search never binds a null title into LOWER().
     */
    @Transactional(readOnly = true)
    public List<Deal> list(String q) {
        UUID tenantId = TenantContext.requireTenantId();
        if (q == null || q.isBlank()) {
            return deals.findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(tenantId);
        }
        return deals.searchByTitle(tenantId, q.trim());
    }

    @Transactional(readOnly = true)
    public Deal get(UUID id) {
        return deals.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Deal not found"));
    }

    @Transactional
    public Deal create(Upsert body) {
        Deal deal = new Deal();
        deal.setId(UUID.randomUUID());
        deal.setTenantId(TenantContext.requireTenantId());
        apply(deal, body);
        if (deal.getOwnerUserId() == null) {
            deal.setOwnerUserId(TenantContext.userId());
        }
        deals.save(deal);
        auditService.record(deal.getTenantId(), TenantContext.userId(), "deal.created", "deal", deal.getId(), deal.getTitle());
        return deal;
    }

    @Transactional
    public Deal update(UUID id, Upsert body) {
        Deal deal = get(id);
        apply(deal, body);
        auditService.record(deal.getTenantId(), TenantContext.userId(), "deal.updated", "deal", deal.getId(), deal.getTitle());
        return deal;
    }

    @Transactional
    public Deal move(UUID id, Deal.Stage stage) {
        Deal deal = get(id);
        deal.setStage(stage);
        auditService.record(deal.getTenantId(), TenantContext.userId(), "deal.stage", "deal", deal.getId(), stage.name());
        return deal;
    }

    @Transactional
    public void delete(UUID id) {
        Deal deal = get(id);
        deal.setDeletedAt(Instant.now());
    }

    private void apply(Deal deal, Upsert body) {
        if (body.title() != null) {
            deal.setTitle(body.title());
        }
        if (deal.getTitle() == null || deal.getTitle().isBlank()) {
            throw ApiException.badRequest("Deal title is required");
        }
        if (body.stage() != null) {
            deal.setStage(body.stage());
        }
        if (body.amount() != null) {
            deal.setAmount(body.amount());
        }
        if (body.currency() != null) {
            deal.setCurrency(body.currency());
        }
        if (body.companyId() != null) {
            deal.setCompanyId(body.companyId());
        }
        if (body.contactId() != null) {
            deal.setContactId(body.contactId());
        }
        if (body.ownerUserId() != null) {
            deal.setOwnerUserId(body.ownerUserId());
        }
        if (body.expectedCloseDate() != null) {
            deal.setExpectedCloseDate(body.expectedCloseDate());
        }
        if (body.notes() != null) {
            deal.setNotes(body.notes());
        }
    }

    public record Upsert(
            String title,
            Deal.Stage stage,
            BigDecimal amount,
            String currency,
            UUID companyId,
            UUID contactId,
            UUID ownerUserId,
            LocalDate expectedCloseDate,
            String notes) {}
}

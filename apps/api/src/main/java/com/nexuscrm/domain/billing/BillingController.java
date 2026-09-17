package com.nexuscrm.domain.billing;

import com.nexuscrm.common.ApiException;
import com.nexuscrm.domain.audit.AuditService;
import com.nexuscrm.domain.tenant.Tenant;
import com.nexuscrm.domain.tenant.TenantRepository;
import com.nexuscrm.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final TenantRepository tenants;
    private final AuditService auditService;

    public BillingController(TenantRepository tenants, AuditService auditService) {
        this.tenants = tenants;
        this.auditService = auditService;
    }

    @GetMapping("/plan")
    public PlanView plan() {
        Tenant tenant = tenants.findById(TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Workspace not found"));
        return new PlanView(tenant.getPlan().name(), List.of("FREE", "PRO", "BUSINESS"), false);
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Transactional
    public CheckoutView checkout(@RequestBody CheckoutBody body) {
        Tenant tenant = tenants.findById(TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Workspace not found"));
        Tenant.Plan plan = Tenant.Plan.valueOf(body.plan());
        tenant.setPlan(plan);
        auditService.record(tenant.getId(), TenantContext.userId(), "billing.stub_checkout", "tenant", tenant.getId(), plan.name());
        return new CheckoutView(
                UUID.randomUUID(),
                plan.name(),
                "STUB",
                "Stripe is not wired in v1. Plan updated locally so sales demos can switch tiers.");
    }

    public record PlanView(String current, List<String> available, boolean stripeEnabled) {}
    public record CheckoutBody(String plan) {}
    public record CheckoutView(UUID sessionId, String plan, String status, String message) {}
}

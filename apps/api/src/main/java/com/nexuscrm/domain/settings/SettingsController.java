package com.nexuscrm.domain.settings;

import com.nexuscrm.common.ApiException;
import com.nexuscrm.domain.audit.AuditService;
import com.nexuscrm.domain.membership.Membership;
import com.nexuscrm.domain.membership.MembershipRepository;
import com.nexuscrm.domain.tenant.Tenant;
import com.nexuscrm.domain.tenant.TenantRepository;
import com.nexuscrm.domain.user.AppUser;
import com.nexuscrm.domain.user.UserRepository;
import com.nexuscrm.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {
    private final UserRepository users;
    private final TenantRepository tenants;
    private final MembershipRepository memberships;
    private final AuditService auditService;

    public SettingsController(
            UserRepository users,
            TenantRepository tenants,
            MembershipRepository memberships,
            AuditService auditService) {
        this.users = users;
        this.tenants = tenants;
        this.memberships = memberships;
        this.auditService = auditService;
    }

    @GetMapping("/profile")
    public ProfileView profile() {
        AppUser user = users.findById(TenantContext.userId()).orElseThrow(() -> ApiException.notFound("User not found"));
        return new ProfileView(user.getId(), user.getEmail(), user.getFullName());
    }

    @PatchMapping("/profile")
    @Transactional
    public ProfileView updateProfile(@RequestBody ProfileBody body) {
        AppUser user = users.findById(TenantContext.userId()).orElseThrow(() -> ApiException.notFound("User not found"));
        if (body.fullName() != null && !body.fullName().isBlank()) {
            user.setFullName(body.fullName().trim());
        }
        return new ProfileView(user.getId(), user.getEmail(), user.getFullName());
    }

    @GetMapping("/tenant")
    public TenantView tenant() {
        Tenant tenant = tenants.findById(TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Workspace not found"));
        return TenantView.from(tenant);
    }

    @PatchMapping("/tenant")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Transactional
    public TenantView updateTenant(@RequestBody TenantBody body) {
        Tenant tenant = tenants.findById(TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Workspace not found"));
        if (body.name() != null && !body.name().isBlank()) {
            tenant.setName(body.name().trim());
        }
        if (body.logoUrl() != null) {
            tenant.setLogoUrl(body.logoUrl());
        }
        auditService.record(tenant.getId(), TenantContext.userId(), "tenant.updated", "tenant", tenant.getId(), tenant.getName());
        return TenantView.from(tenant);
    }

    @GetMapping("/team")
    public List<MemberView> team() {
        UUID tenantId = TenantContext.requireTenantId();
        return memberships.findByTenantId(tenantId).stream().map(m -> {
            AppUser user = users.findById(m.getUserId()).orElse(null);
            return new MemberView(
                    m.getId(),
                    m.getUserId(),
                    user == null ? "" : user.getEmail(),
                    user == null ? "" : user.getFullName(),
                    m.getRole().name());
        }).toList();
    }

    @PatchMapping("/team/{membershipId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @Transactional
    public MemberView updateRole(@PathVariable UUID membershipId, @RequestBody RoleBody body) {
        Membership membership = memberships.findByIdAndTenantId(membershipId, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Membership not found"));
        if (membership.getRole() == Membership.Role.OWNER && body.role() != Membership.Role.OWNER) {
            throw ApiException.badRequest("Transfer ownership is not supported in v1");
        }
        membership.setRole(body.role());
        AppUser user = users.findById(membership.getUserId()).orElseThrow();
        auditService.record(membership.getTenantId(), TenantContext.userId(), "membership.role", "membership",
                membership.getId(), body.role().name());
        return new MemberView(membership.getId(), user.getId(), user.getEmail(), user.getFullName(), membership.getRole().name());
    }

    public record ProfileView(UUID id, String email, String fullName) {}
    public record ProfileBody(String fullName) {}
    public record TenantBody(String name, String logoUrl) {}
    public record TenantView(UUID id, String name, String slug, String logoUrl, String plan) {
        static TenantView from(Tenant t) {
            return new TenantView(t.getId(), t.getName(), t.getSlug(), t.getLogoUrl(), t.getPlan().name());
        }
    }
    public record MemberView(UUID membershipId, UUID userId, String email, String fullName, String role) {}
    public record RoleBody(Membership.Role role) {}
}

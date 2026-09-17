package com.nexuscrm.domain.invite;

import com.nexuscrm.common.ApiException;
import com.nexuscrm.domain.audit.AuditService;
import com.nexuscrm.domain.membership.Membership;
import com.nexuscrm.domain.membership.MembershipRepository;
import com.nexuscrm.domain.tenant.Tenant;
import com.nexuscrm.domain.tenant.TenantRepository;
import com.nexuscrm.domain.user.AppUser;
import com.nexuscrm.domain.user.UserRepository;
import com.nexuscrm.tenant.BaseEntity;
import com.nexuscrm.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "invites")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
class Invite extends BaseEntity {
    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(nullable = false)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Membership.Role role;
    @Column(nullable = false, unique = true)
    private String token;
    @Column(name = "invited_by")
    private UUID invitedBy;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Override
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Membership.Role getRole() { return role; }
    public void setRole(Membership.Role role) { this.role = role; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UUID getInvitedBy() { return invitedBy; }
    public void setInvitedBy(UUID invitedBy) { this.invitedBy = invitedBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
}

interface InviteRepository extends JpaRepository<Invite, UUID> {
    List<Invite> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<Invite> findByToken(String token);
}

@Service
class InviteService {
    private final InviteRepository invites;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final TenantRepository tenants;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    InviteService(
            InviteRepository invites,
            UserRepository users,
            MembershipRepository memberships,
            TenantRepository tenants,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.invites = invites;
        this.users = users;
        this.memberships = memberships;
        this.tenants = tenants;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    List<Invite> list() {
        return invites.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId());
    }

    @Transactional
    Invite create(String email, Membership.Role role) {
        if (role == Membership.Role.OWNER) {
            throw ApiException.badRequest("Cannot invite an OWNER");
        }
        Invite invite = new Invite();
        invite.setId(UUID.randomUUID());
        invite.setTenantId(TenantContext.requireTenantId());
        invite.setEmail(email.trim().toLowerCase(Locale.ROOT));
        invite.setRole(role);
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        invite.setToken(HexFormat.of().formatHex(bytes));
        invite.setInvitedBy(TenantContext.userId());
        invite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invites.save(invite);
        auditService.record(invite.getTenantId(), TenantContext.userId(), "invite.created", "invite", invite.getId(), email);
        return invite;
    }

    @Transactional
    UUID accept(String token, String fullName, String password) {
        Invite invite = invites.findByToken(token).orElseThrow(() -> ApiException.notFound("Invite not found"));
        if (invite.getAcceptedAt() != null || invite.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("Invite is expired or already used");
        }
        Tenant tenant = tenants.findById(invite.getTenantId()).orElseThrow(() -> ApiException.notFound("Workspace not found"));
        AppUser user = users.findByEmailIgnoreCase(invite.getEmail()).orElseGet(() -> {
            AppUser created = new AppUser(UUID.randomUUID(), invite.getEmail(), passwordEncoder.encode(password), fullName);
            return users.save(created);
        });
        memberships.findByTenantIdAndUserId(tenant.getId(), user.getId())
                .orElseGet(() -> memberships.save(new Membership(UUID.randomUUID(), tenant.getId(), user.getId(), invite.getRole())));
        invite.setAcceptedAt(Instant.now());
        return user.getId();
    }
}

@RestController
@RequestMapping("/api/v1/invites")
public class InviteController {
    private final InviteService invites;

    public InviteController(InviteService invites) {
        this.invites = invites;
    }

    @GetMapping
    public List<InviteView> list() {
        return invites.list().stream().map(InviteView::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteView create(@RequestBody CreateBody body) {
        return InviteView.from(invites.create(body.email(), body.role()));
    }

    @PostMapping("/accept")
    public AcceptView accept(@RequestBody AcceptBody body) {
        UUID userId = invites.accept(body.token(), body.fullName(), body.password());
        return new AcceptView(userId, "Invite accepted. You can now sign in.");
    }

    public record CreateBody(String email, Membership.Role role) {}
    public record AcceptBody(String token, String fullName, String password) {}
    public record AcceptView(UUID userId, String message) {}
    public record InviteView(UUID id, String email, String role, Instant expiresAt, Instant acceptedAt, String token) {
        static InviteView from(Invite i) {
            return new InviteView(i.getId(), i.getEmail(), i.getRole().name(), i.getExpiresAt(), i.getAcceptedAt(), i.getToken());
        }
    }
}

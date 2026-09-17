package com.nexuscrm.domain.auth;

import com.nexuscrm.common.ApiException;
import com.nexuscrm.config.NexusProperties;
import com.nexuscrm.domain.audit.AuditService;
import com.nexuscrm.domain.membership.Membership;
import com.nexuscrm.domain.membership.MembershipRepository;
import com.nexuscrm.domain.tenant.Tenant;
import com.nexuscrm.domain.tenant.TenantRepository;
import com.nexuscrm.domain.user.AppUser;
import com.nexuscrm.domain.user.UserRepository;
import com.nexuscrm.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern SLUG = Pattern.compile("[^a-z0-9]+");

    private final UserRepository users;
    private final TenantRepository tenants;
    private final MembershipRepository memberships;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NexusProperties properties;
    private final AuditService auditService;

    public AuthService(
            UserRepository users,
            TenantRepository tenants,
            MembershipRepository memberships,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            NexusProperties properties,
            AuditService auditService) {
        this.users = users;
        this.tenants = tenants;
        this.memberships = memberships;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.auditService = auditService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with that email already exists");
        }
        String slug = uniqueSlug(req.organizationName());
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        AppUser user = new AppUser(userId, email, passwordEncoder.encode(req.password()), req.fullName().trim());
        Tenant tenant = new Tenant(tenantId, req.organizationName().trim(), slug);
        tenant.setPlan(Tenant.Plan.FREE);
        users.save(user);
        tenants.save(tenant);
        memberships.save(new Membership(UUID.randomUUID(), tenantId, userId, Membership.Role.OWNER));
        auditService.record(tenantId, userId, "tenant.created", "tenant", tenantId, tenant.getName());
        return issue(user, tenant, Membership.Role.OWNER);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        List<Membership> ms = memberships.findByUserId(user.getId());
        if (ms.isEmpty()) {
            throw ApiException.forbidden("No workspace memberships for this account");
        }
        Membership membership = ms.getFirst();
        if (req.tenantSlug() != null && !req.tenantSlug().isBlank()) {
            Tenant wanted = tenants.findBySlug(req.tenantSlug())
                    .orElseThrow(() -> ApiException.notFound("Workspace not found"));
            membership = memberships.findByTenantIdAndUserId(wanted.getId(), user.getId())
                    .orElseThrow(() -> ApiException.forbidden("Not a member of that workspace"));
        }
        Tenant tenant = tenants.findById(membership.getTenantId())
                .orElseThrow(() -> ApiException.notFound("Workspace not found"));
        auditService.record(tenant.getId(), user.getId(), "auth.login", "user", user.getId(), user.getEmail());
        return issue(user, tenant, membership.getRole());
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (JwtException e) {
            throw ApiException.unauthorized("Invalid refresh token");
        }
        if (!"refresh".equals(claims.get("typ"))) {
            throw ApiException.unauthorized("Invalid refresh token");
        }
        String hash = sha256(refreshToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("Refresh token revoked"));
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.unauthorized("Refresh token expired");
        }
        stored.setRevokedAt(Instant.now());
        AppUser user = users.findById(stored.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        Tenant tenant = tenants.findById(stored.getTenantId())
                .orElseThrow(() -> ApiException.unauthorized("Workspace not found"));
        Membership membership = memberships.findByTenantIdAndUserId(tenant.getId(), user.getId())
                .orElseThrow(() -> ApiException.forbidden("Membership ended"));
        return issue(user, tenant, membership.getRole());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(sha256(refreshToken)).ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    public MeResponse me(UUID userId, UUID tenantId) {
        AppUser user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Tenant tenant = tenants.findById(tenantId).orElseThrow(() -> ApiException.notFound("Workspace not found"));
        Membership membership = memberships.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> ApiException.forbidden("Not a member"));
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getLogoUrl(),
                tenant.getPlan().name(),
                membership.getRole().name());
    }

    private TokenResponse issue(AppUser user, Tenant tenant, Membership.Role role) {
        UUID refreshId = UUID.randomUUID();
        String access = jwtService.createAccessToken(user.getId(), user.getEmail(), tenant.getId(), role.name());
        String refresh = jwtService.createRefreshToken(user.getId(), tenant.getId(), refreshId);
        RefreshToken entity = new RefreshToken();
        entity.setId(refreshId);
        entity.setUserId(user.getId());
        entity.setTenantId(tenant.getId());
        entity.setTokenHash(sha256(refresh));
        entity.setExpiresAt(Instant.now().plus(properties.getJwt().getRefreshTtl()));
        refreshTokens.save(entity);
        return new TokenResponse(
                access,
                refresh,
                properties.getJwt().getAccessTtl().toSeconds(),
                me(user.getId(), tenant.getId()));
    }

    private String uniqueSlug(String name) {
        String base = SLUG.matcher(name.toLowerCase(Locale.ROOT).trim()).replaceAll("-");
        base = base.replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "workspace";
        }
        String slug = base;
        int i = 1;
        while (tenants.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record RegisterRequest(String email, String password, String fullName, String organizationName) {}

    public record LoginRequest(String email, String password, String tenantSlug) {}

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, MeResponse user) {}

    public record MeResponse(
            UUID userId,
            String email,
            String fullName,
            UUID tenantId,
            String tenantName,
            String tenantSlug,
            String logoUrl,
            String plan,
            String role) {}
}

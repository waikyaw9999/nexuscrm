package com.nexuscrm.security;

import com.nexuscrm.config.NexusProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private final NexusProperties properties;
    private final SecretKey key;

    public JwtService(NexusProperties properties) {
        this.properties = properties;
        byte[] bytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(UUID userId, String email, UUID tenantId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getJwt().getAccessTtl());
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of(
                        "email", email,
                        "tenantId", tenantId.toString(),
                        "role", role,
                        "typ", "access"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(UUID userId, UUID tenantId, UUID tokenId) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getJwt().getRefreshTtl());
        return Jwts.builder()
                .subject(userId.toString())
                .id(tokenId.toString())
                .claims(Map.of(
                        "tenantId", tenantId.toString(),
                        "typ", "refresh"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

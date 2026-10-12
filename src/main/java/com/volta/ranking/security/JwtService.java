package com.volta.ranking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final Environment environment;

    @Value("${volta.jwt.secret}")
    private String secret;

    @Value("${volta.jwt.expiration}")
    private long expiration;

    // ─── Geração ─────────────────────────────────────────────────────────────

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities()
                .stream().map(Object::toString).toList());
        return buildToken(claims, userDetails);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // jjwt 0.12.x: Jwts.builder() com .signWith(key) sem especificar algoritmo
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ─── Validação ────────────────────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration() != null
                && claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Supports both the primary API JWT (single role claim) and the local demo JWT (roles list).
     */
    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        if (!StringUtils.hasText(claims.getSubject()) || claims.getExpiration() == null) {
            return List.of();
        }

        Object role = claims.get("role");
        if (role instanceof String roleName) {
            try {
                UUID.fromString(claims.get("userId", String.class));
                UUID.fromString(claims.get("companyId", String.class));
            } catch (RuntimeException exception) {
                return List.of();
            }
            return authoritiesFor(roleName);
        }

        Object roles = claims.get("roles");
        if (environment.acceptsProfiles(Profiles.of("local")) && roles instanceof Collection<?> roleNames) {
            return roleNames.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .flatMap(roleName -> authoritiesFor(roleName).stream())
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    private List<GrantedAuthority> authoritiesFor(String role) {
        return switch (role) {
            case "ADMIN", "ROLE_ADMIN" -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case "MANAGER", "GESTOR", "ROLE_GESTOR" -> List.of(new SimpleGrantedAuthority("ROLE_GESTOR"));
            case "EMPLOYEE", "OPERATOR", "FUNCIONARIO", "ROLE_FUNCIONARIO" ->
                    List.of(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"));
            default -> List.of();
        };
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        // jjwt 0.12.x: Jwts.parser() em vez de Jwts.parserBuilder()
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

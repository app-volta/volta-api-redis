package com.volta.ranking.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public final class TestTokens {

    private TestTokens() {
    }

    public static String voltaApiToken(String secret, String role, UUID companyId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 60_000);

        return Jwts.builder()
                .subject(role.toLowerCase() + "@volta.com")
                .claim("userId", UUID.randomUUID().toString())
                .claim("companyId", companyId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}

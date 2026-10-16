package com.volta.ranking.security;

import com.volta.ranking.security.jwt.TokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenProviderTest {

    private static final String KEY = "chave-de-teste-com-mais-de-256-bits-para-o-algoritmo-hs512-1234567890";

    private final UUID userId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final AuthenticatedUser user = new AuthenticatedUser(userId, "manager@volta.com", null, companyId, "MANAGER");

    @Test
    void generatesATokenWithTheIdentityClaims() {
        TokenProvider tokenProvider = new TokenProvider(KEY, 3_600_000L);

        String token = tokenProvider.generateToken(authenticationOf(user));
        Optional<Claims> claims = tokenProvider.parseClaims(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("manager@volta.com");
        assertThat(claims.get().get("userId", String.class)).isEqualTo(userId.toString());
        assertThat(claims.get().get("companyId", String.class)).isEqualTo(companyId.toString());
        assertThat(claims.get().get("role", String.class)).isEqualTo("MANAGER");
        assertThat(claims.get().getExpiration()).isNotNull();
    }

    @Test
    void exposesTheExpirationTime() {
        assertThat(new TokenProvider(KEY, 3_600_000L).getExpirationTime()).isEqualTo(3_600_000L);
    }

    @Test
    void rejectsAnExpiredToken() {
        TokenProvider tokenProvider = new TokenProvider(KEY, -1_000L);

        String token = tokenProvider.generateToken(authenticationOf(user));

        assertThat(tokenProvider.parseClaims(token)).isEmpty();
    }

    @Test
    void rejectsATokenSignedWithAnotherKey() {
        TokenProvider other = new TokenProvider("outra-chave-de-teste-com-mais-de-256-bits-0987654321-abcdef", 3_600_000L);
        TokenProvider tokenProvider = new TokenProvider(KEY, 3_600_000L);

        String token = other.generateToken(authenticationOf(user));

        assertThat(tokenProvider.parseClaims(token)).isEmpty();
    }

    @Test
    void rejectsGarbageAndBlankTokens() {
        TokenProvider tokenProvider = new TokenProvider(KEY, 3_600_000L);

        assertThat(tokenProvider.parseClaims("garbage.not.valid")).isEmpty();
        assertThat(tokenProvider.parseClaims("")).isEmpty();
    }

    private Authentication authenticationOf(AuthenticatedUser principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}

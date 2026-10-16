package com.volta.ranking.security;

import com.volta.ranking.security.filter.JwtAuthenticationFilter;
import com.volta.ranking.security.jwt.TokenProvider;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String KEY = "chave-de-teste-com-mais-de-256-bits-para-o-algoritmo-hs512-1234567890";

    private final TokenProvider tokenProvider = new TokenProvider(KEY, 3_600_000L);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);
    private final UUID userId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesAValidToken() throws Exception {
        String token = token("manager@volta.com", userId.toString(), companyId.toString(), "MANAGER", true);

        MockFilterChain chain = run("Bearer " + token);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(
                new AuthenticatedUser(userId, "manager@volta.com", null, companyId, "MANAGER"));
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("MANAGER");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void ignoresARequestWithoutAuthorizationHeader() throws Exception {
        run(null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresAHeaderThatIsNotBearer() throws Exception {
        run("Basic dXNlcjpwYXNz");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresAnInvalidToken() throws Exception {
        run("Bearer garbage.not.valid");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresATokenWithoutIdentityClaims() throws Exception {
        run("Bearer " + token("manager@volta.com", null, companyId.toString(), "MANAGER", true));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        run("Bearer " + token("manager@volta.com", userId.toString(), null, "MANAGER", true));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        run("Bearer " + token("manager@volta.com", userId.toString(), companyId.toString(), null, true));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresATokenWithoutExpiration() throws Exception {
        run("Bearer " + token("manager@volta.com", userId.toString(), companyId.toString(), "MANAGER", false));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresATokenWithMalformedUuids() throws Exception {
        run("Bearer " + token("manager@volta.com", "not-a-uuid", companyId.toString(), "MANAGER", true));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockFilterChain run(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }

        filter.doFilter(request, response, chain);
        return chain;
    }

    private String token(String subject, String userIdClaim, String companyIdClaim, String role, boolean withExpiration) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8)));

        if (userIdClaim != null) {
            builder.claim("userId", userIdClaim);
        }
        if (companyIdClaim != null) {
            builder.claim("companyId", companyIdClaim);
        }
        if (role != null) {
            builder.claim("role", role);
        }
        if (withExpiration) {
            builder.expiration(new Date(now.getTime() + 60_000));
        }

        return builder.compact();
    }
}

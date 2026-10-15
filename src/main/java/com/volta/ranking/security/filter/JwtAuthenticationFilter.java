package com.volta.ranking.security.filter;

import com.volta.ranking.security.AuthenticatedUser;
import com.volta.ranking.security.jwt.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            Optional<Claims> claims = tokenProvider.parseClaims(token);

            if (claims.isPresent()) {
                Optional<AuthenticatedUser> principal = toPrincipal(claims.get());

                if (principal.isPresent()) {
                    AuthenticatedUser user = principal.get();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<AuthenticatedUser> toPrincipal(Claims claims) {
        String userId = claims.get("userId", String.class);
        String companyId = claims.get("companyId", String.class);
        String role = claims.get("role", String.class);

        if (!StringUtils.hasText(claims.getSubject())
                || !StringUtils.hasText(userId)
                || !StringUtils.hasText(companyId)
                || !StringUtils.hasText(role)) {
            return Optional.empty();
        }

        if (claims.getExpiration() == null) {
            return Optional.empty();
        }

        try {
            AuthenticatedUser user = new AuthenticatedUser(
                    UUID.fromString(userId),
                    claims.getSubject(),
                    null,
                    UUID.fromString(companyId),
                    role
            );
            return Optional.of(user);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}

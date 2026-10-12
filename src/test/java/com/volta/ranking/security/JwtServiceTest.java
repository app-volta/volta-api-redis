package com.volta.ranking.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa geração e validação do token sem subir o contexto Spring.
 * A chave usada aqui é só para teste — nunca a mesma de produção.
 */
class JwtServiceTest {

    private static final String CHAVE_TESTE =
            "chave-de-teste-com-mais-de-256-bits-para-o-algoritmo-hs512-1234567890";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        jwtService = new JwtService(environment);
        ReflectionTestUtils.setField(jwtService, "secret", CHAVE_TESTE);
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L); // 1h
    }

    @Test
    void generateToken_eIsTokenValid_aceitamTokenDoMesmoUsuario() {
        UserDetails admin = usuario("admin", "ROLE_ADMIN");

        String token = jwtService.generateToken(admin);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.isTokenValid(token, admin)).isTrue();
    }

    @Test
    void isTokenValid_comUsuarioDiferenteDoToken_retornaFalso() {
        UserDetails admin = usuario("admin", "ROLE_ADMIN");
        UserDetails gestor = usuario("gestor", "ROLE_GESTOR");

        String token = jwtService.generateToken(admin);

        assertThat(jwtService.isTokenValid(token, gestor)).isFalse();
    }

    @Test
    void isTokenValid_comTokenExpirado_lancaExpiredJwtException() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1_000L); // já expirado
        UserDetails admin = usuario("admin", "ROLE_ADMIN");

        String token = jwtService.generateToken(admin);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, admin))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void extractAuthorities_aceitaRoleDoTokenDaVoltaApi() {
        String token = Jwts.builder()
                .subject("funcionario@volta.com")
                .claim("userId", "550e8400-e29b-41d4-a716-446655440000")
                .claim("companyId", "660e8400-e29b-41d4-a716-446655440000")
                .claim("role", "EMPLOYEE")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(CHAVE_TESTE.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.extractUsername(token)).isEqualTo("funcionario@volta.com");
        assertThat(jwtService.extractAuthorities(token))
                .extracting("authority")
                .containsExactly("ROLE_FUNCIONARIO");
    }

    @Test
    void extractAuthorities_mapeiaManagerParaPermissoesDeGestor() {
        String token = Jwts.builder()
                .subject("gestor@volta.com")
                .claim("userId", "550e8400-e29b-41d4-a716-446655440000")
                .claim("companyId", "660e8400-e29b-41d4-a716-446655440000")
                .claim("role", "MANAGER")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(CHAVE_TESTE.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.extractAuthorities(token))
                .extracting("authority")
                .containsExactly("ROLE_GESTOR");
    }

    @Test
    void extractAuthorities_rejeitaTokenPrincipalSemExpiracaoOuClaimsDeIdentidade() {
        String semExpiracao = Jwts.builder()
                .subject("admin@volta.com")
                .claim("userId", "550e8400-e29b-41d4-a716-446655440000")
                .claim("companyId", "660e8400-e29b-41d4-a716-446655440000")
                .claim("role", "ADMIN")
                .signWith(Keys.hmacShaKeyFor(CHAVE_TESTE.getBytes(StandardCharsets.UTF_8)))
                .compact();
        String semEmpresa = Jwts.builder()
                .subject("admin@volta.com")
                .claim("userId", "550e8400-e29b-41d4-a716-446655440000")
                .claim("role", "ADMIN")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(CHAVE_TESTE.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.extractAuthorities(semExpiracao)).isEmpty();
        assertThat(jwtService.extractAuthorities(semEmpresa)).isEmpty();
    }

    @Test
    void extractAuthorities_rejeitaTokenLocalForaDoPerfilLocal() {
        String token = jwtService.generateToken(usuario("admin", "ROLE_ADMIN"));
        ReflectionTestUtils.setField(jwtService, "environment", new MockEnvironment());

        assertThat(jwtService.extractAuthorities(token)).isEmpty();
    }

    private UserDetails usuario(String username, String role) {
        return User.builder()
                .username(username)
                .password("irrelevante-para-o-teste")
                .authorities(role)
                .build();
    }
}

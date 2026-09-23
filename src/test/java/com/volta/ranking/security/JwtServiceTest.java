package com.volta.ranking.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

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
        jwtService = new JwtService();
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

    private UserDetails usuario(String username, String role) {
        return User.builder()
                .username(username)
                .password("irrelevante-para-o-teste")
                .authorities(role)
                .build();
    }
}

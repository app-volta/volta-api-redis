package com.volta.ranking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Usuários em memória para demonstração e testes da API.
 *
 * Em produção, substituir pelo UserRepository do PostgreSQL
 * compartilhado com a volta-api principal.
 *
 * Roles do VOLTA:
 *   FUNCIONARIO → apenas leitura do ranking
 *   GESTOR      → leitura + atualizar scores
 *   ADMIN       → acesso total
 */
@Configuration
public class InMemoryUsersConfig {

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.builder()
                .username("funcionario")
                .password(encoder.encode("senha123"))
                .roles("FUNCIONARIO")
                .build(),
            User.builder()
                .username("gestor")
                .password(encoder.encode("senha123"))
                .roles("GESTOR")
                .build(),
            User.builder()
                .username("admin")
                .password(encoder.encode("senha123"))
                .roles("ADMIN")
                .build()
        );
    }
}

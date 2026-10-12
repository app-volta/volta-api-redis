package com.volta.ranking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Usuários em memória de demonstração, carregados apenas no perfil local.
 *
 * Roles do VOLTA:
 *   FUNCIONARIO → apenas leitura do ranking
 *   GESTOR      → leitura + atualizar scores
 *   ADMIN       → acesso total
 */
@Configuration
@Profile("local")
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

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

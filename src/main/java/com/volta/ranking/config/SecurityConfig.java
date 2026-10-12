package com.volta.ranking.config;

import tools.jackson.databind.ObjectMapper;
import com.volta.ranking.security.JwtAuthenticationFilter;
import com.volta.ranking.security.RestAuthHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectMapper objectMapper) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {

                // ── Públicos ──────────────────────────────────────────────
                if (environment.acceptsProfiles(Profiles.of("local"))) {
                    auth.requestMatchers("/api/v1/auth/**").permitAll();
                } else {
                    auth.requestMatchers("/api/v1/auth/**").denyAll();
                }
                auth.requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**"
                ).permitAll();
                auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                auth.requestMatchers("/error").permitAll();

                // ── Leitura: qualquer usuário autenticado ─────────────────
                auth.requestMatchers(HttpMethod.GET, "/api/v1/ranking/**")
                    .hasAnyRole("FUNCIONARIO", "GESTOR", "ADMIN");

                // ── Escrita: apenas GESTOR e ADMIN ────────────────────────
                auth.requestMatchers(HttpMethod.POST, "/api/v1/ranking/**")
                    .hasAnyRole("GESTOR", "ADMIN");

                // ── Remoção: apenas ADMIN ─────────────────────────────────
                auth.requestMatchers(HttpMethod.DELETE, "/api/v1/ranking/**")
                    .hasRole("ADMIN");

                // ── Localização: leitura para usuários; sincronização por ADMIN ──
                auth.requestMatchers(HttpMethod.GET, "/api/v1/cooperatives/nearby")
                    .hasAnyRole("FUNCIONARIO", "GESTOR", "ADMIN");
                auth.requestMatchers(HttpMethod.PUT, "/api/v1/cooperatives/*/location")
                    .hasRole("ADMIN");
                auth.requestMatchers(HttpMethod.DELETE, "/api/v1/cooperatives/*/location")
                    .hasRole("ADMIN");

                auth.anyRequest().authenticated();
            })
            .exceptionHandling(e -> {
                RestAuthHandler handler = new RestAuthHandler(objectMapper);
                e.authenticationEntryPoint(handler).accessDeniedHandler(handler);
            })
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}

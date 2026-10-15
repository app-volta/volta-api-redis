package com.volta.ranking.config;

import com.volta.ranking.handler.RestAccessDeniedHandler;
import com.volta.ranking.handler.RestAuthenticationEntryPoint;
import com.volta.ranking.security.filter.JwtAuthenticationFilter;
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
public class SecurityConfiguration {

    private static final String[] READ_AUTHORITIES = {"ADMIN", "MANAGER", "EMPLOYEE", "OPERATOR"};
    private static final String[] WRITE_AUTHORITIES = {"ADMIN", "MANAGER"};
    private static final String ADMIN_AUTHORITY = "ADMIN";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean localProfile = environment.acceptsProfiles(Profiles.of("local"));

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    if (localProfile) {
                        auth.requestMatchers("/api/v1/auth/**").permitAll();
                        auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll();
                    } else {
                        auth.requestMatchers("/api/v1/auth/**").denyAll();
                        auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").denyAll();
                    }

                    auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll();
                    auth.requestMatchers("/error").permitAll();

                    auth.requestMatchers(HttpMethod.GET, "/api/v1/ranking/**").hasAnyAuthority(READ_AUTHORITIES);
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/ranking/**").hasAnyAuthority(WRITE_AUTHORITIES);
                    auth.requestMatchers(HttpMethod.PUT, "/api/v1/ranking/**").hasAuthority(ADMIN_AUTHORITY);
                    auth.requestMatchers(HttpMethod.DELETE, "/api/v1/ranking/**").hasAuthority(ADMIN_AUTHORITY);

                    auth.requestMatchers(HttpMethod.GET, "/api/v1/cooperatives/nearby")
                            .hasAnyAuthority(READ_AUTHORITIES);
                    auth.requestMatchers(HttpMethod.PUT, "/api/v1/cooperatives/*/location")
                            .hasAuthority(ADMIN_AUTHORITY);
                    auth.requestMatchers(HttpMethod.DELETE, "/api/v1/cooperatives/*/location")
                            .hasAuthority(ADMIN_AUTHORITY);

                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

package com.volta.ranking.config;

import com.volta.ranking.security.AuthenticatedUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Configuration
@Profile("local")
public class LocalUsersConfiguration {

    public static final UUID DEMO_COMPANY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final String DEMO_PASSWORD = "senha123";

    @Bean
    public UserDetailsService localUserDetailsService(PasswordEncoder passwordEncoder) {
        String encodedPassword = passwordEncoder.encode(DEMO_PASSWORD);
        Map<String, AuthenticatedUser> users = new HashMap<>();

        users.put("employee@volta.com", demoUser("employee@volta.com", encodedPassword, "EMPLOYEE"));
        users.put("manager@volta.com", demoUser("manager@volta.com", encodedPassword, "MANAGER"));
        users.put("admin@volta.com", demoUser("admin@volta.com", encodedPassword, "ADMIN"));

        return email -> {
            AuthenticatedUser user = users.get(email.toLowerCase(Locale.ROOT));

            if (user == null) {
                throw new UsernameNotFoundException("User not found");
            }

            return user;
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService localUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(localUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    private AuthenticatedUser demoUser(String email, String encodedPassword, String role) {
        return new AuthenticatedUser(UUID.randomUUID(), email, encodedPassword, DEMO_COMPANY_ID, role);
    }
}

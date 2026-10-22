package com.volta.ranking.service;

import com.volta.ranking.dto.auth.LoginRequestDTO;
import com.volta.ranking.dto.auth.TokenResponseDTO;
import com.volta.ranking.security.jwt.TokenProvider;
import com.volta.ranking.usecase.AuthUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    public TokenResponseDTO login(LoginRequestDTO dto) {
        UsernamePasswordAuthenticationToken credentials =
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password());
        Authentication authentication = authenticationManager.authenticate(credentials);

        String token = tokenProvider.generateToken(authentication);
        return new TokenResponseDTO(token, tokenProvider.getExpirationTime());
    }
}

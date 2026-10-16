package com.volta.ranking.controller;

import com.volta.ranking.controller.docs.AuthControllerDocs;
import com.volta.ranking.dto.auth.LoginRequestDTO;
import com.volta.ranking.dto.auth.TokenResponseDTO;
import com.volta.ranking.usecase.AuthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthControllerDocs {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO dto) {
        TokenResponseDTO token = authUseCase.login(dto);
        return token;
    }
}

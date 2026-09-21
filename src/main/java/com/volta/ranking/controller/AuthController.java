package com.volta.ranking.controller;

import com.volta.ranking.dto.ApiResponseDTO;
import com.volta.ranking.dto.AuthRequestDTO;
import com.volta.ranking.dto.AuthResponseDTO;
import com.volta.ranking.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login e geração de token JWT")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(
        summary = "Autenticar e obter token JWT",
        description = """
            Usuários disponíveis para teste:

            | Username    | Senha    | Role        | Acesso                          |
            |-------------|----------|-------------|---------------------------------|
            | funcionario | senha123 | FUNCIONARIO | GET apenas                      |
            | gestor      | senha123 | GESTOR      | GET + POST (atualizar scores)   |
            | admin       | senha123 | ADMIN       | Tudo (incluindo DELETE)         |
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> login(
            @Valid @RequestBody AuthRequestDTO request) {

        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = (UserDetails) auth.getPrincipal();
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(ApiResponseDTO.success(
            "Login realizado com sucesso.",
            AuthResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .build()
        ));
    }
}

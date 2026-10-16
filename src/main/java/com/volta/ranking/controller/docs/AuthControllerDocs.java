package com.volta.ranking.controller.docs;

import com.volta.ranking.dto.auth.LoginRequestDTO;
import com.volta.ranking.dto.auth.TokenResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Autenticação", description = "Login de demonstração, disponível somente no perfil local")
public interface AuthControllerDocs {

    @Operation(
            summary = "Realiza login de demonstração",
            description = "Existe somente no perfil `local`. Usuários: `admin@volta.com`, `manager@volta.com` e "
                    + "`employee@volta.com`, todos com a senha `senha123`. Fora do perfil local, os tokens são os "
                    + "emitidos pela volta-api. **Acesso:** público."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de login inválidos"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos")
    })
    TokenResponseDTO login(LoginRequestDTO dto);
}

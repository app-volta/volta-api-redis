package com.volta.ranking.security;

import tools.jackson.databind.ObjectMapper;
import com.volta.ranking.dto.ApiResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Erros de autenticação/autorização que acontecem no filtro (antes do controller) e por isso
 * não passam pelo GlobalExceptionHandler. Usa o mesmo ApiResponseDTO do resto da API — serializado
 * com ObjectMapper em vez de concatenação de String — para o corpo da resposta ser idêntico,
 * timestamp incluído, não importa qual dos dois caminhos tratou o erro.
 */
public class RestAuthHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAuthHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException ex) throws IOException {
        escrever(response, HttpServletResponse.SC_UNAUTHORIZED, "Autenticação necessária.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException ex) throws IOException {
        escrever(response, HttpServletResponse.SC_FORBIDDEN, "Sem permissão para esta operação.");
    }

    private void escrever(HttpServletResponse response, int status, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponseDTO.error(mensagem));
    }
}

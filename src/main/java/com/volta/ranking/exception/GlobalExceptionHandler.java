package com.volta.ranking.exception;

import com.volta.ranking.dto.ApiResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404 — empresa não encontrada no Redis */
    @ExceptionHandler(EmpresaNaoEncontradaException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleNaoEncontrada(EmpresaNaoEncontradaException ex) {
        log.warn("[RANKING] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    /** 400 — falha de validação (@Valid) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            String campo = ((FieldError) e).getField();
            erros.put(campo, e.getDefaultMessage());
        });

        log.warn("[VALIDAÇÃO] {}", erros);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.<Map<String, String>>builder()
                        .success(false)
                        .message("Erro de validação.")
                        .data(erros)
                        .build());
    }

    /** 400 — argumento inválido */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    /** 400 — validação de parâmetros de rota/query (@Min, @Max, @Pattern) */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponseDTO<List<String>>> handleMethodValidation(
            HandlerMethodValidationException ex) {

        List<String> erros = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();

        log.warn("[VALIDAÇÃO] {}", erros);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.<List<String>>builder()
                        .success(false)
                        .message("Erro de validação.")
                        .data(erros)
                        .build());
    }

    /** 400 — corpo da requisição ausente ou JSON malformado */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleBodyIlegivel(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error("Corpo da requisição inválido ou malformado."));
    }

    /** 400 — tipo de parâmetro incorreto (ex.: limit=abc) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error("Valor inválido para o parâmetro '" + ex.getName() + "'."));
    }

    /** 404 — rota inexistente */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleRotaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.error("Recurso não encontrado."));
    }

    /** 405 — método HTTP não suportado na rota */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponseDTO.error("Método " + ex.getMethod() + " não suportado nesta rota."));
    }

    /** 401 — credenciais inválidas */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("[AUTH] tentativa de login com credenciais inválidas");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDTO.error("Usuário ou senha inválidos."));
    }

    /** 403 — sem permissão */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDTO.error("Sem permissão para esta operação."));
    }

    /** 500 — erro inesperado (inclui falha de conexão com Redis) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGeneric(Exception ex) {
        log.error("[ERRO] {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error("Erro interno. Tente novamente mais tarde."));
    }
}

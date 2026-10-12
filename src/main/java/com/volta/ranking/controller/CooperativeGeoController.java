package com.volta.ranking.controller;

import com.volta.ranking.dto.ApiResponseDTO;
import com.volta.ranking.dto.CooperativeLocationRequestDTO;
import com.volta.ranking.dto.NearbyCooperativeDTO;
import com.volta.ranking.service.CooperativeGeoService;
import com.volta.ranking.validation.ValidationPatterns;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cooperatives")
@RequiredArgsConstructor
@Tag(name = "Cooperativas", description = "Índice geoespacial de cooperativas no Redis")
@SecurityRequirement(name = "bearerAuth")
public class CooperativeGeoController {

    private final CooperativeGeoService cooperativeGeoService;

    @PutMapping("/{cooperativeUuid}/location")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adicionar ou atualizar a localização de uma cooperativa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localização atualizada"),
            @ApiResponse(responseCode = "400", description = "UUID ou coordenadas inválidas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Requer ADMIN")
    })
    public ResponseEntity<ApiResponseDTO<Void>> saveLocation(
            @Parameter(description = "UUID da cooperativa")
            @Pattern(regexp = ValidationPatterns.UUID, message = ValidationPatterns.UUID_MESSAGE)
            @PathVariable String cooperativeUuid,
            @Valid @RequestBody CooperativeLocationRequestDTO request
    ) {
        cooperativeGeoService.saveLocation(
                UUID.fromString(cooperativeUuid), request.latitude(), request.longitude()
        );
        return ResponseEntity.ok(ApiResponseDTO.success("Localização da cooperativa atualizada.", null));
    }

    @DeleteMapping("/{cooperativeUuid}/location")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover a localização de uma cooperativa do índice GEO")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localização removida"),
            @ApiResponse(responseCode = "400", description = "UUID inválido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Requer ADMIN")
    })
    public ResponseEntity<ApiResponseDTO<Void>> removeLocation(
            @Parameter(description = "UUID da cooperativa")
            @Pattern(regexp = ValidationPatterns.UUID, message = ValidationPatterns.UUID_MESSAGE)
            @PathVariable String cooperativeUuid
    ) {
        cooperativeGeoService.removeLocation(UUID.fromString(cooperativeUuid));
        return ResponseEntity.ok(ApiResponseDTO.success("Localização da cooperativa removida.", null));
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','GESTOR','ADMIN')")
    @Operation(
            summary = "Buscar cooperativas próximas",
            description = "Busca UUIDs e distância em quilômetros, ordenados do mais próximo. " +
                    "A volta-api deve fornecer as coordenadas da empresa autenticada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busca concluída"),
            @ApiResponse(responseCode = "400", description = "Coordenadas, raio ou limite inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<ApiResponseDTO<List<NearbyCooperativeDTO>>> findNearby(
            @Parameter(description = "Latitude da empresa", example = "-23.550520")
            @RequestParam
            @DecimalMin("-85.051128") @DecimalMax("85.051128")
            BigDecimal latitude,
            @Parameter(description = "Longitude da empresa", example = "-46.633308")
            @RequestParam
            @DecimalMin("-180.0") @DecimalMax("180.0")
            BigDecimal longitude,
            @Parameter(description = "Raio da busca em quilômetros", example = "50")
            @RequestParam
            @DecimalMin(value = "0", inclusive = false) @DecimalMax("20000")
            BigDecimal radiusKm,
            @Parameter(description = "Máximo de cooperativas retornadas (1–100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100)
            int limit
    ) {
        List<NearbyCooperativeDTO> cooperatives = cooperativeGeoService.findNearby(
                latitude, longitude, radiusKm, limit
        );
        return ResponseEntity.ok(ApiResponseDTO.success(
                "Busca de cooperativas próximas concluída. Total: " + cooperatives.size() + ".",
                cooperatives
        ));
    }
}

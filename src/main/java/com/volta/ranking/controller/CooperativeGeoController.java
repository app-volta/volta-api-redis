package com.volta.ranking.controller;

import com.volta.ranking.controller.docs.CooperativeGeoControllerDocs;
import com.volta.ranking.dto.cooperative.CooperativeLocationRequestDTO;
import com.volta.ranking.dto.cooperative.NearbyCooperativeResponseDTO;
import com.volta.ranking.usecase.CooperativeGeoUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cooperatives")
public class CooperativeGeoController implements CooperativeGeoControllerDocs {

    private final CooperativeGeoUseCase cooperativeGeoUseCase;

    @PutMapping("/{cooperativeId}/location")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveLocation(
            @PathVariable UUID cooperativeId,
            @Valid @RequestBody CooperativeLocationRequestDTO dto
    ) {
        cooperativeGeoUseCase.saveLocation(cooperativeId, dto);
    }

    @DeleteMapping("/{cooperativeId}/location")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLocation(@PathVariable UUID cooperativeId) {
        cooperativeGeoUseCase.removeLocation(cooperativeId);
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OPERATOR')")
    public List<NearbyCooperativeResponseDTO> findNearby(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam BigDecimal radiusKm,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<NearbyCooperativeResponseDTO> cooperatives =
                cooperativeGeoUseCase.findNearby(latitude, longitude, radiusKm, limit);
        return cooperatives;
    }
}

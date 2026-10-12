package com.volta.ranking.dto;

import java.util.UUID;

public record NearbyCooperativeDTO(UUID cooperativeUuid, double distanceKm) {
}

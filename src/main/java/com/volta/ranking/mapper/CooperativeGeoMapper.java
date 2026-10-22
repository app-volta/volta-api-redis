package com.volta.ranking.mapper;

import com.volta.ranking.dto.cooperative.NearbyCooperativeResponseDTO;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CooperativeGeoMapper {

    public List<NearbyCooperativeResponseDTO> toResponses(GeoResults<RedisGeoCommands.GeoLocation<String>> results) {
        List<NearbyCooperativeResponseDTO> responses = new ArrayList<>();

        if (results == null) {
            return responses;
        }

        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results.getContent()) {
            UUID cooperativeId = UUID.fromString(result.getContent().getName());
            double distanceKm = result.getDistance().getValue();

            responses.add(new NearbyCooperativeResponseDTO(cooperativeId, distanceKm));
        }

        return responses;
    }
}

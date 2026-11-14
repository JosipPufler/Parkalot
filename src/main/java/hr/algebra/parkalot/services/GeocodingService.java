package hr.algebra.parkalot.services;

import hr.algebra.parkalot.configs.GeometryFactoryProvider;
import hr.algebra.parkalot.exceptions.LocalizedBadRequestException;
import hr.algebra.parkalot.models.entities.Household;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final RestClient.Builder restClientBuilder;

    private RestClient createClient() {
        return restClientBuilder
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader(
                        HttpHeaders.USER_AGENT,
                        "Parkalot/1.0 (student project)"
                )
                .build();
    }

    public Point geocode(Household household) {

        List<Map<String, Object>> results = createClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("street", household.getStreet() + " " + household.getHouseNumber())
                        .queryParam("city", household.getCity())
                        .queryParam("postalcode", household.getPostalCode())
                        .queryParam("country", household.getCountry())
                        .queryParam("format", "jsonv2")
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (results == null || results.isEmpty()) {
            throw new LocalizedBadRequestException(
                    LocalizedExceptionKeys.ADDRESS_NOT_LOCATED
            );
        }

        double latitude = Double.parseDouble(
                results.getFirst().get("lat").toString()
        );

        double longitude = Double.parseDouble(
                results.getFirst().get("lon").toString()
        );

        return GeometryFactoryProvider.geometryFactory
                .createPoint(new Coordinate(longitude, latitude));
    }
}

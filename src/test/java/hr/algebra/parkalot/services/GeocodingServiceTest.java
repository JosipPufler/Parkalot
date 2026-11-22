package hr.algebra.parkalot.services;

import hr.algebra.parkalot.exceptions.LocalizedBadRequestException;
import hr.algebra.parkalot.models.entities.Household;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Unit test: the Nominatim HTTP call is replaced by a mock server, never hits the network. */
class GeocodingServiceTest {
    private MockRestServiceServer server;
    private GeocodingService service;
    private Household household;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GeocodingService(builder);
        household = Household.builder().street("Ilica").houseNumber("5").city("Zagreb")
                .postalCode("10000").country("Croatia").build();
    }

    @Test
    void geocode_returnsPointWithLongitudeAsX() {
        server.expect(requestTo(startsWith("https://nominatim.openstreetmap.org/search")))
                .andExpect(queryParam("city", "Zagreb"))
                .andRespond(withSuccess("[{\"lat\": \"45.81\", \"lon\": \"15.97\"}]", MediaType.APPLICATION_JSON));

        Point point = service.geocode(household);

        assertThat(point.getY()).isEqualTo(45.81);
        assertThat(point.getX()).isEqualTo(15.97);
        assertThat(point.getSRID()).isEqualTo(4326);
        server.verify();
    }

    @Test
    void geocode_withNoResults_throwsBadRequest() {
        server.expect(requestTo(startsWith("https://nominatim.openstreetmap.org/search")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.geocode(household)).isInstanceOf(LocalizedBadRequestException.class);
    }
}

package hr.algebra.parkalot.integration;

import hr.algebra.parkalot.AbstractIntegrationTest;
import hr.algebra.parkalot.helper.TestData;
import hr.algebra.parkalot.models.entities.Area;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.repositories.AreaRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static hr.algebra.parkalot.helper.TestData.auth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AreaControllerTest extends AbstractIntegrationTest {
    @Autowired private TestData data;
    @Autowired private AreaRepo areaRepo;

    private User user;
    private User admin;
    private Area area;

    @BeforeEach
    void setUp() {
        user = data.user("user@test.com", Role.USER);
        admin = data.user("admin@test.com", Role.ADMIN);
        area = data.area("Centar", 15.0, 45.0, 1.0);
    }

    private static String areaJson(Long id, String name, double minLon, double minLat, double size) {
        return """
                {"id": %s, "name": "%s", "zone": "ZONE_2",
                 "coordinates": [
                   {"latitude": %s, "longitude": %s},
                   {"latitude": %s, "longitude": %s},
                   {"latitude": %s, "longitude": %s}],
                 "rule": {"requiresResidentAddress": true, "maxVehiclesPerHousehold": 2, "emissionClass": "EURO_4"}}
                """.formatted(id, name,
                minLat, minLon,
                minLat + size, minLon,
                minLat + size, minLon + size);
    }

    @Test
    void getAllAreas_returnsAreasForAnyRole() throws Exception {
        mockMvc.perform(get("/rest/area").with(auth(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Centar"))
                .andExpect(jsonPath("$[0].rule.maxVehiclesPerHousehold").value(5));
    }

    @Test
    void getAllAreas_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/rest/area")).andExpect(status().isUnauthorized());
    }

    @Test
    void whereAmI_insideArea_returnsArea() throws Exception {
        mockMvc.perform(get("/rest/area/whereami").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\": 45.5, \"longitude\": 15.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Centar"));
    }

    @Test
    void whereAmI_outsideAnyArea_returnsNotFound() throws Exception {
        mockMvc.perform(get("/rest/area/whereami").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\": 10.0, \"longitude\": 10.0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void whereAmI_insideOverlappingAreas_returnsFirst() throws Exception {
        // Overlaps are normally prevented by the API, but the service tolerates them if data is imported.
        data.area("Overlap", 15.2, 45.2, 0.5);

        mockMvc.perform(get("/rest/area/whereami").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\": 45.3, \"longitude\": 15.3}"))
                .andExpect(status().isOk());
    }

    @Test
    void createArea_asAdmin_persistsAreaWithRule() throws Exception {
        mockMvc.perform(post("/rest/area").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(areaJson(null, "Novi", 20.0, 40.0, 1.0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novi"))
                .andExpect(jsonPath("$.rule.emissionClass").value("EURO_4"));

        assertThat(areaRepo.count()).isEqualTo(2);
    }

    @Test
    void createArea_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(post("/rest/area").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(areaJson(null, "Novi", 20.0, 40.0, 1.0)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createArea_withTooFewCoordinates_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/rest/area").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Bad", "zone": "ZONE_1",
                                 "coordinates": [{"latitude": 1, "longitude": 1}],
                                 "rule": {"requiresResidentAddress": false, "maxVehiclesPerHousehold": 1, "emissionClass": "EURO_1"}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createArea_overlappingExistingArea_isRejected() throws Exception {
        mockMvc.perform(post("/rest/area").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(areaJson(null, "Overlap", 15.5, 45.5, 1.0)))
                .andExpect(status().isConflict());

        assertThat(areaRepo.count()).isEqualTo(1);
    }

    @Test
    void updateArea_asAdmin_updatesFields() throws Exception {
        mockMvc.perform(put("/rest/area").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(areaJson(area.getId(), "Renamed", 15.0, 45.0, 1.0)))
                .andExpect(status().isOk());

        data.flushAndClear();
        Area updated = areaRepo.findById(area.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Renamed");
        assertThat(updated.getEligibilityRule().getMaxVehiclesPerHousehold()).isEqualTo(2);
        assertThat(updated.getEligibilityRule().getRequiresResidentAddress()).isTrue();
    }

    @Test
    void updateArea_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(put("/rest/area").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(areaJson(999999L, "Ghost", 30.0, 30.0, 1.0)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateArea_overlappingAnotherArea_isRejected() throws Exception {
        Area other = data.area("Other", 30.0, 30.0, 1.0);

        mockMvc.perform(put("/rest/area").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(areaJson(other.getId(), "Other", 15.5, 45.5, 1.0)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteArea_asAdmin_removesArea() throws Exception {
        mockMvc.perform(delete("/rest/area/{id}", area.getId()).with(auth(admin)))
                .andExpect(status().isOk());

        assertThat(areaRepo.existsById(area.getId())).isFalse();
    }

    @Test
    void deleteArea_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/rest/area/999999").with(auth(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteArea_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/rest/area/{id}", area.getId()).with(auth(user)))
                .andExpect(status().isForbidden());
    }
}

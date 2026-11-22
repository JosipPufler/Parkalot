package hr.algebra.parkalot.integration;

import hr.algebra.parkalot.AbstractIntegrationTest;
import hr.algebra.parkalot.helper.TestData;
import hr.algebra.parkalot.models.entities.Area;
import hr.algebra.parkalot.models.entities.Permit;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.models.enums.EmissionClass;
import hr.algebra.parkalot.models.enums.PermitDuration;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.repositories.VehicleRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static hr.algebra.parkalot.helper.TestData.auth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VehicleControllerTest extends AbstractIntegrationTest {
    @Autowired private TestData data;
    @Autowired private VehicleRepo vehicleRepo;

    private User user;
    private User other;
    private User admin;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        user = data.user("user@test.com", Role.USER);
        other = data.user("other@test.com", Role.USER);
        admin = data.user("admin@test.com", Role.ADMIN);
        vehicle = data.vehicle("ZG1111AA", EmissionClass.EURO_5, user);
        data.vehicle("ZG2222BB", EmissionClass.EURO_5, other);
    }

    private String vehicleJson(Long id, String plate, String ownerIds) {
        return """
                {"id": %s, "make": "Skoda", "model": "Octavia", "year": 2021, "licencePlate": "%s",
                 "ownerIds": %s, "emissionClass": "EURO_6"}
                """.formatted(id, plate, ownerIds);
    }

    @Test
    void getAllVehicles_userSeesOwn_adminSeesAll() throws Exception {
        mockMvc.perform(get("/rest/vehicle").with(auth(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].licencePlate").value("ZG1111AA"));
        mockMvc.perform(get("/rest/vehicle").with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllVehicles_officerIsForbidden() throws Exception {
        User officer = data.user("officer@test.com", Role.OFFICER);
        mockMvc.perform(get("/rest/vehicle").with(auth(officer))).andExpect(status().isForbidden());
    }

    @Test
    void getVehicle_ownerGetsDetailsWithArchive() throws Exception {
        Area area = data.area("Centar", 15, 45, 1);
        Permit old = data.permit(vehicle, area, user, LocalDate.now().minusDays(30), PermitDuration.DAY, true);
        data.archive(old);

        mockMvc.perform(get("/rest/vehicle/ZG1111AA").with(auth(user)))
                .andExpect(status().isOk());
    }

    @Test
    void getVehicle_notOwnedOrUnknown_returnsNotFound() throws Exception {
        mockMvc.perform(get("/rest/vehicle/ZG2222BB").with(auth(user))).andExpect(status().isNotFound());
        mockMvc.perform(get("/rest/vehicle/NOPE").with(auth(user))).andExpect(status().isNotFound());
    }

    @Test
    void registerVehicle_makesCurrentUserPrimaryOwner() throws Exception {
        mockMvc.perform(post("/rest/vehicle").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(null, "ZG9999ZZ", "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licencePlate").value("ZG9999ZZ"))
                .andExpect(jsonPath("$.ownerIds[0]").value(user.getId()));

        assertThat(vehicleRepo.findByLicencePlate("ZG9999ZZ")).isPresent();
    }

    @Test
    void createVehicle_asAdmin_assignsOwners() throws Exception {
        mockMvc.perform(post("/rest/vehicle/insert").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(null, "ZG9999ZZ", "[%d, %d, %d]".formatted(user.getId(), other.getId(), user.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerIds.length()").value(2));
    }

    @Test
    void createVehicle_unknownOwner_returnsNotFound() throws Exception {
        mockMvc.perform(post("/rest/vehicle/insert").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(null, "ZG9999ZZ", "[999999]")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createVehicle_asUser_isForbidden() throws Exception {
        mockMvc.perform(post("/rest/vehicle/insert").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(null, "ZG9999ZZ", "[]")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateVehicle_replacesOwnersAndFields() throws Exception {
        // user is removed, other is added and becomes the primary owner.
        mockMvc.perform(put("/rest/vehicle").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(vehicle.getId(), "ZG1111AA", "[%d]".formatted(other.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.make").value("Skoda"))
                .andExpect(jsonPath("$.ownerIds.length()").value(1))
                .andExpect(jsonPath("$.ownerIds[0]").value(other.getId()));
    }

    @Test
    void updateVehicle_keepingPrimaryOwnerAndAddingAnother() throws Exception {
        mockMvc.perform(put("/rest/vehicle").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(vehicle.getId(), "ZG1111AA", "[%d, %d]".formatted(user.getId(), other.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerIds.length()").value(2));

        Vehicle reloaded = vehicleRepo.findById(vehicle.getId()).orElseThrow();
        assertThat(reloaded.getOwnerships()).filteredOn(o -> o.getPrimaryOwner()).hasSize(1)
                .allMatch(o -> o.getUser().getId().equals(user.getId()));
    }

    @Test
    void updateVehicle_withNoOwners_removesAllOwnerships() throws Exception {
        mockMvc.perform(put("/rest/vehicle").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(vehicle.getId(), "ZG1111AA", "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerIds.length()").value(0));
    }

    @Test
    void updateVehicle_unknownVehicleOrOwner_returnsNotFound() throws Exception {
        mockMvc.perform(put("/rest/vehicle").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(999999L, "X", "[]")))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/rest/vehicle").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson(vehicle.getId(), "ZG1111AA", "[999999]")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteVehicle_adminOnly() throws Exception {
        data.flushAndClear();
        mockMvc.perform(delete("/rest/vehicle/{id}", vehicle.getId()).with(auth(user))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/rest/vehicle/{id}", vehicle.getId()).with(auth(admin))).andExpect(status().isOk());
        data.flushAndClear();
        assertThat(vehicleRepo.existsById(vehicle.getId())).isFalse();
        mockMvc.perform(delete("/rest/vehicle/{id}", vehicle.getId()).with(auth(admin))).andExpect(status().isNotFound());
    }
}

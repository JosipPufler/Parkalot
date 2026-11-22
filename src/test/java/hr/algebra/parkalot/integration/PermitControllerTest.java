package hr.algebra.parkalot.integration;

import hr.algebra.parkalot.AbstractIntegrationTest;
import hr.algebra.parkalot.helper.TestData;
import hr.algebra.parkalot.models.dtos.PermitRequestDto;
import hr.algebra.parkalot.models.entities.*;
import hr.algebra.parkalot.models.enums.EmissionClass;
import hr.algebra.parkalot.models.enums.PermitDuration;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.models.enums.Zone;
import hr.algebra.parkalot.repositories.PermitArchiveRepo;
import hr.algebra.parkalot.repositories.PermitRepo;
import hr.algebra.parkalot.services.PayPalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Optional;

import static hr.algebra.parkalot.helper.TestData.auth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PermitControllerTest extends AbstractIntegrationTest {
    @Autowired private TestData data;
    @Autowired private PermitRepo permitRepo;
    @Autowired private PermitArchiveRepo permitArchiveRepo;

    @MockitoBean private PayPalService payPalService;

    private Household household;
    private User user;
    private User other;
    private User admin;
    private User officer;
    private Vehicle vehicle;
    private Vehicle otherVehicle;
    private Area area;

    @BeforeEach
    void setUp() {
        // Area covers lon 15..16, lat 45..46; the household lives inside it.
        household = data.household("Inside street", 15.5, 45.5);
        user = data.userInHousehold("user@test.com", Role.USER, household);
        other = data.user("other@test.com", Role.USER);
        admin = data.user("admin@test.com", Role.ADMIN);
        officer = data.user("officer@test.com", Role.OFFICER);
        vehicle = data.vehicle("ZG1111AA", EmissionClass.EURO_6, user);
        otherVehicle = data.vehicle("ZG2222BB", EmissionClass.EURO_6, other);
        area = data.area("Centar", Zone.ZONE_1, 15.0, 45.0, 1.0, true, 1, EmissionClass.EURO_4);
    }

    private String requestJson(Long vehicleId, Long areaId, LocalDate start) {
        return """
                {"vehicleId": %d, "areaId": %d, "startDate": "%s", "permitDuration": "DAY"}
                """.formatted(vehicleId, areaId, start);
    }

    // ---- GET /rest/permits/{plate} ----

    @Test
    void getPermitsForVehicle_owner_returnsPermits() throws Exception {
        data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, true);

        mockMvc.perform(get("/rest/permits/ZG1111AA").with(auth(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPermitsForVehicle_officerAndAdmin_canSeeAnyVehicle() throws Exception {
        mockMvc.perform(get("/rest/permits/ZG1111AA").with(auth(officer))).andExpect(status().isOk());
        mockMvc.perform(get("/rest/permits/ZG1111AA").with(auth(admin))).andExpect(status().isOk());
    }

    @Test
    void getPermitsForVehicle_notOwnedOrUnknown_returnsNotFound() throws Exception {
        mockMvc.perform(get("/rest/permits/ZG2222BB").with(auth(user))).andExpect(status().isNotFound());
        mockMvc.perform(get("/rest/permits/NOPE").with(auth(user))).andExpect(status().isNotFound());
    }

    // ---- GET /rest/permits ----

    @Test
    void getPermits_user_returnsOnlyOwnVehiclesPermits() throws Exception {
        data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, true);
        data.permit(otherVehicle, area, other, LocalDate.now(), PermitDuration.DAY, true);

        mockMvc.perform(get("/rest/permits").with(auth(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPermits_admin_returnsAllPermits() throws Exception {
        data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, true);
        data.permit(otherVehicle, area, other, LocalDate.now(), PermitDuration.DAY, true);

        mockMvc.perform(get("/rest/permits").with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getPermits_officer_isForbidden() throws Exception {
        mockMvc.perform(get("/rest/permits").with(auth(officer))).andExpect(status().isForbidden());
    }

    // ---- POST /rest/permits ----

    @Test
    void requestPermit_valid_createsUnpaidPermitWithOrderId() throws Exception {
        when(payPalService.createPermitOrder(any(PermitRequestDto.class))).thenReturn(Optional.of("ORDER-1"));

        mockMvc.perform(post("/rest/permits").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(vehicle.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER-1"));

        Permit saved = permitRepo.findByPaypalId("ORDER-1").orElseThrow();
        assertThat(saved.getPaymentTime()).isNull();
        assertThat(saved.getPrice()).isEqualTo(15.0);
        assertThat(saved.getBuyer().getId()).isEqualTo(user.getId());
    }

    @Test
    void requestPermit_unknownVehicleOrArea_returnsNotFound() throws Exception {
        mockMvc.perform(post("/rest/permits").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(999999L, area.getId(), LocalDate.now())))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/rest/permits").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(vehicle.getId(), 999999L, LocalDate.now())))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestPermit_overlappingExistingPermit_returnsBadRequest() throws Exception {
        data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.WEEK, true);

        mockMvc.perform(post("/rest/permits").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(vehicle.getId(), area.getId(), LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPermit_paypalFails_returnsBadRequest() throws Exception {
        when(payPalService.createPermitOrder(any(PermitRequestDto.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/rest/permits").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(vehicle.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isBadRequest());
        assertThat(permitRepo.count()).isZero();
    }

    @Test
    void requestPermit_userWithoutHousehold_returnsBadRequest() throws Exception {
        Vehicle otherCar = data.vehicle("ZG3333CC", EmissionClass.EURO_6, other);

        mockMvc.perform(post("/rest/permits").with(auth(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(otherCar.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPermit_vehicleTooPolluting_returnsBadRequest() throws Exception {
        Vehicle old = data.vehicle("ZG4444DD", EmissionClass.EURO_2, user);

        mockMvc.perform(post("/rest/permits").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(old.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPermit_areaWithoutRule_returnsBadRequest() throws Exception {
        Area noRule = data.saveArea(Area.builder()
                .name("NoRule")
                .zone(Zone.ZONE_3)
                .areaPolygon(data.square(30, 30, 1))
                .build());

        mockMvc.perform(post("/rest/permits").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(vehicle.getId(), noRule.getId(), LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPermit_householdOutsideResidentArea_returnsBadRequest() throws Exception {
        Household far = data.household("Far street", 50.0, 10.0);
        User resident = data.userInHousehold("far@test.com", Role.USER, far);
        Vehicle car = data.vehicle("ZG5555EE", EmissionClass.EURO_6, resident);

        mockMvc.perform(post("/rest/permits").with(auth(resident))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(car.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPermit_householdVehicleLimitReached_returnsBadRequest() throws Exception {
        Vehicle second = data.vehicle("ZG6666FF", EmissionClass.EURO_6, user);
        data.permit(vehicle, area, user, LocalDate.now().minusDays(1), PermitDuration.WEEK, true);

        mockMvc.perform(post("/rest/permits").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(second.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPermit_officer_isForbidden() throws Exception {
        mockMvc.perform(post("/rest/permits").with(auth(officer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(vehicle.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isForbidden());
    }

    // ---- POST /rest/permits/admin ----

    @Test
    void createAdminPermit_createsPaidPermitForVehicleOwner() throws Exception {
        mockMvc.perform(post("/rest/permits/admin").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(vehicle.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isOk());

        assertThat(permitRepo.findAll()).singleElement().satisfies(p -> {
            assertThat(p.getPaymentTime()).isNotNull();
            assertThat(p.getBuyer().getId()).isEqualTo(user.getId());
        });
    }

    @Test
    void createAdminPermit_errors() throws Exception {
        mockMvc.perform(post("/rest/permits/admin").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(999999L, area.getId(), LocalDate.now())))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/rest/permits/admin").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(vehicle.getId(), 999999L, LocalDate.now())))
                .andExpect(status().isNotFound());

        Vehicle orphan = data.vehicle("ZG7777GG", EmissionClass.EURO_6);
        mockMvc.perform(post("/rest/permits/admin").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(orphan.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isNotFound());

        data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, true);
        mockMvc.perform(post("/rest/permits/admin").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(vehicle.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAdminPermit_asUser_isForbidden() throws Exception {
        mockMvc.perform(post("/rest/permits/admin").with(auth(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(vehicle.getId(), area.getId(), LocalDate.now())))
                .andExpect(status().isForbidden());
    }

    // ---- PUT / DELETE ----

    private String updateJson(Long id, Long areaId, Long vehicleId, LocalDate start) {
        return """
                {"id": %d, "startDate": "%s", "permitDuration": "WEEK", "areaId": %d, "vehicleId": %d}
                """.formatted(id, start, areaId, vehicleId);
    }

    @Test
    void updatePermit_asAdmin_updatesPermit() throws Exception {
        Permit permit = data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, true);
        LocalDate newStart = LocalDate.now().plusDays(3);

        mockMvc.perform(put("/rest/permits").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson(permit.getId(), area.getId(), vehicle.getId(), newStart)))
                .andExpect(status().isOk());

        Permit updated = permitRepo.findById(permit.getId()).orElseThrow();
        assertThat(updated.getStartDate()).isEqualTo(newStart);
        assertThat(updated.getPermitDuration()).isEqualTo(PermitDuration.WEEK);
    }

    @Test
    void updatePermit_missingReferences_returnNotFound() throws Exception {
        Permit permit = data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, true);
        LocalDate start = LocalDate.now();

        mockMvc.perform(put("/rest/permits").with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(999999L, area.getId(), vehicle.getId(), start))).andExpect(status().isNotFound());
        mockMvc.perform(put("/rest/permits").with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(permit.getId(), 999999L, vehicle.getId(), start))).andExpect(status().isNotFound());
        mockMvc.perform(put("/rest/permits").with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(permit.getId(), area.getId(), 999999L, start))).andExpect(status().isNotFound());

        Vehicle orphan = data.vehicle("ZG8888HH", EmissionClass.EURO_6);
        mockMvc.perform(put("/rest/permits").with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(permit.getId(), area.getId(), orphan.getId(), start))).andExpect(status().isNotFound());
    }

    @Test
    void updatePermit_asUser_isForbidden() throws Exception {
        Permit permit = data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, true);

        mockMvc.perform(put("/rest/permits").with(auth(user)).contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(permit.getId(), area.getId(), vehicle.getId(), LocalDate.now())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePermit_asAdmin_removesIt_andUnknownIs404() throws Exception {
        Permit permit = data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, true);
        data.flushAndClear();

        mockMvc.perform(delete("/rest/permits/{id}", permit.getId()).with(auth(admin))).andExpect(status().isOk());
        data.flushAndClear();
        assertThat(permitRepo.existsById(permit.getId())).isFalse();
        mockMvc.perform(delete("/rest/permits/999999").with(auth(admin))).andExpect(status().isNotFound());
    }

    @Test
    void deletePermit_asUser_isForbidden() throws Exception {
        Permit permit = data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, true);

        mockMvc.perform(delete("/rest/permits/{id}", permit.getId()).with(auth(user))).andExpect(status().isForbidden());
    }

    // ---- /rest/permits/past ----

    @Test
    void pastPermits_forVehicleAndForUser() throws Exception {
        Permit expired = data.permit(vehicle, area, user, LocalDate.now().minusDays(10), PermitDuration.DAY, true);
        data.archive(expired);

        mockMvc.perform(get("/rest/permits/past/ZG1111AA").with(auth(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/rest/permits/past/ZG2222BB").with(auth(user)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/rest/permits/past").with(auth(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deletePastPermit_adminOnly() throws Exception {
        Permit expired = data.permit(vehicle, area, user, LocalDate.now().minusDays(10), PermitDuration.DAY, true);
        PermitArchive archive = data.archive(expired);

        mockMvc.perform(delete("/rest/permits/past/{id}", archive.getId()).with(auth(user))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/rest/permits/past/{id}", archive.getId()).with(auth(admin))).andExpect(status().isOk());
        mockMvc.perform(delete("/rest/permits/past/{id}", archive.getId()).with(auth(admin))).andExpect(status().isNotFound());
        assertThat(permitArchiveRepo.existsById(archive.getId())).isFalse();
    }
}

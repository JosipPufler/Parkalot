package hr.algebra.parkalot.integration;

import hr.algebra.parkalot.AbstractIntegrationTest;
import hr.algebra.parkalot.helper.TestData;
import hr.algebra.parkalot.models.entities.*;
import hr.algebra.parkalot.models.enums.EmissionClass;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.repositories.AppealEvidenceRepo;
import hr.algebra.parkalot.repositories.AppealRepo;
import hr.algebra.parkalot.repositories.ViolationRepo;
import hr.algebra.parkalot.services.PayPalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static hr.algebra.parkalot.helper.TestData.auth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ViolationControllerTest extends AbstractIntegrationTest {
    @Autowired private TestData data;
    @Autowired private ViolationRepo violationRepo;
    @Autowired private AppealRepo appealRepo;
    @Autowired private AppealEvidenceRepo evidenceRepo;

    @MockitoBean private PayPalService payPalService;

    private User user;
    private User other;
    private User admin;
    private User officer;
    private Area area;
    private Violation violation;

    @BeforeEach
    void setUp() {
        user = data.user("user@test.com", Role.USER);
        other = data.user("other@test.com", Role.USER);
        admin = data.user("admin@test.com", Role.ADMIN);
        officer = data.user("officer@test.com", Role.OFFICER);
        data.vehicle("ZG1111AA", EmissionClass.EURO_6, user);
        area = data.area("Centar", 15, 45, 1);
        violation = data.violation(officer, "ZG1111AA", area, true);
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile("evidence", name, "image/png", new byte[]{1, 2, 3});
    }

    // ---- listing ----

    @Test
    void getViolations_userSeesOwnVehiclesViolations_adminSeesAll() throws Exception {
        data.violation(officer, "UNOWNED", area, true);

        mockMvc.perform(get("/rest/violations").with(auth(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/rest/violations").with(auth(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getVehicleViolations_byPlate() throws Exception {
        mockMvc.perform(get("/rest/violations/vehicle").param("licensePlate", "ZG1111AA").with(auth(officer)))
                .andExpect(status().isOk());
    }

    // ---- paying ----

    @Test
    void resolveViolation_ownerOfActiveViolation_createsPaymentOrder() throws Exception {
        when(payPalService.createViolationPaymentOrder(any())).thenReturn("VIOL-ORDER");

        mockMvc.perform(post("/rest/violations").param("violationId", violation.getId().toString()).with(auth(user)))
                .andExpect(status().isOk());

        assertThat(violationRepo.findById(violation.getId()).orElseThrow().getOrderId()).isEqualTo("VIOL-ORDER");
    }

    @Test
    void resolveViolation_errors() throws Exception {
        mockMvc.perform(post("/rest/violations").param("violationId", "999999").with(auth(user)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/rest/violations").param("violationId", violation.getId().toString()).with(auth(other)))
                .andExpect(status().isNotFound());

        Violation inactive = data.violation(officer, "ZG1111AA", area, false);
        mockMvc.perform(post("/rest/violations").param("violationId", inactive.getId().toString()).with(auth(user)))
                .andExpect(status().isBadRequest());
    }

    // ---- admin CRUD ----

    @Test
    void updateViolation_adminOnly() throws Exception {
        String body = """
                {"id": %d, "message": "updated", "active": false}
                """.formatted(violation.getId());

        mockMvc.perform(put("/rest/violations").with(auth(user)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/rest/violations").with(auth(admin)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        Violation updated = violationRepo.findById(violation.getId()).orElseThrow();
        assertThat(updated.getMessage()).isEqualTo("updated");
        assertThat(updated.getActive()).isFalse();
    }

    @Test
    void deleteViolation_adminOnly() throws Exception {
        Violation extra = data.violation(officer, "ZG1111AA", area, true);
        data.flushAndClear();

        mockMvc.perform(delete("/rest/violations/{id}", extra.getId()).with(auth(user))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/rest/violations/{id}", 999999).with(auth(admin))).andExpect(status().isNotFound());
        mockMvc.perform(delete("/rest/violations/{id}", extra.getId()).with(auth(admin))).andExpect(status().isOk());
    }

    // ---- appeals ----

    @Test
    void raiseAppeal_withEvidence_savesAppealAndFiles() throws Exception {
        mockMvc.perform(multipart("/rest/violations/appeal")
                        .file(file("a.png")).file(file("b.png"))
                        .param("violationId", violation.getId().toString())
                        .param("title", "Unfair").param("message", "I had a permit")
                        .with(auth(user)))
                .andExpect(status().isOk());

        assertThat(appealRepo.findByUserId(user.getId())).hasSize(1);
        assertThat(evidenceRepo.count()).isEqualTo(2);
    }

    @Test
    void raiseAppeal_withoutEvidence_savesAppeal() throws Exception {
        mockMvc.perform(multipart("/rest/violations/appeal")
                        .param("violationId", violation.getId().toString())
                        .param("title", "Unfair").param("message", "I had a permit")
                        .with(auth(user)))
                .andExpect(status().isOk());

        assertThat(appealRepo.findByUserId(user.getId())).hasSize(1);
    }

    @Test
    void raiseAppeal_notFoundOrNotOwner() throws Exception {
        mockMvc.perform(multipart("/rest/violations/appeal")
                        .param("violationId", "999999").param("title", "t").param("message", "m")
                        .with(auth(user)))
                .andExpect(status().isNotFound());
        mockMvc.perform(multipart("/rest/violations/appeal")
                        .param("violationId", violation.getId().toString()).param("title", "t").param("message", "m")
                        .with(auth(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void raiseAppeal_asOfficer_isForbidden() throws Exception {
        mockMvc.perform(multipart("/rest/violations/appeal")
                        .param("violationId", violation.getId().toString()).param("title", "t").param("message", "m")
                        .with(auth(officer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminInsertAppeal_forOwner() throws Exception {
        mockMvc.perform(multipart("/rest/violations/appeal/insert")
                        .file(file("a.png"))
                        .param("violationId", violation.getId().toString())
                        .param("ownerId", user.getId().toString())
                        .param("title", "Admin made").param("message", "m")
                        .with(auth(admin)))
                .andExpect(status().isOk());
        assertThat(appealRepo.findByUserId(user.getId())).hasSize(1);

        mockMvc.perform(multipart("/rest/violations/appeal/insert")
                        .param("violationId", violation.getId().toString())
                        .param("ownerId", user.getId().toString())
                        .param("title", "No evidence").param("message", "m")
                        .with(auth(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void adminInsertAppeal_errors() throws Exception {
        mockMvc.perform(multipart("/rest/violations/appeal/insert")
                        .param("violationId", violation.getId().toString()).param("ownerId", "999999")
                        .param("title", "t").param("message", "m").with(auth(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(multipart("/rest/violations/appeal/insert")
                        .param("violationId", "999999").param("ownerId", user.getId().toString())
                        .param("title", "t").param("message", "m").with(auth(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(multipart("/rest/violations/appeal/insert")
                        .param("violationId", violation.getId().toString()).param("ownerId", other.getId().toString())
                        .param("title", "t").param("message", "m").with(auth(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(multipart("/rest/violations/appeal/insert")
                        .param("violationId", violation.getId().toString()).param("ownerId", user.getId().toString())
                        .param("title", "t").param("message", "m").with(auth(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAppeals_dependsOnRole() throws Exception {
        data.appeal(user, violation);

        mockMvc.perform(get("/rest/violations/appeal").with(auth(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/rest/violations/appeal").with(auth(other)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/rest/violations/appeal").with(auth(officer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/rest/violations/appeal").with(auth(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void approveAppeal_deactivatesViolation_andCannotBeResolvedTwice() throws Exception {
        Appeal appeal = data.appeal(user, violation);

        mockMvc.perform(put("/rest/violations/appeal/resolve/{id}", appeal.getId()).with(auth(officer)))
                .andExpect(status().isOk());

        assertThat(appealRepo.findById(appeal.getId()).orElseThrow().getResolved()).isTrue();
        assertThat(violationRepo.findById(violation.getId()).orElseThrow().getActive()).isFalse();

        mockMvc.perform(put("/rest/violations/appeal/resolve/{id}", appeal.getId()).with(auth(officer)))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/rest/violations/appeal/resolve/999999").with(auth(officer)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/rest/violations/appeal/resolve/{id}", appeal.getId()).with(auth(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectAppeal_marksRejectedAndResolved_andCannotBeRepeated() throws Exception {
        Appeal appeal = data.appeal(user, violation);

        mockMvc.perform(put("/rest/violations/appeal/reject/{id}", appeal.getId()).with(auth(admin)))
                .andExpect(status().isOk());

        Appeal reloaded = appealRepo.findById(appeal.getId()).orElseThrow();
        assertThat(reloaded.getRejected()).isTrue();
        assertThat(reloaded.getResolved()).isTrue();
        assertThat(violationRepo.findById(violation.getId()).orElseThrow().getActive()).isTrue();

        mockMvc.perform(put("/rest/violations/appeal/reject/{id}", appeal.getId()).with(auth(admin)))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/rest/violations/appeal/reject/999999").with(auth(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadEvidence_ownerAndStaffAllowed_othersUnauthorized() throws Exception {
        Appeal appeal = data.appeal(user, violation);
        AppealEvidence evidence = data.evidence(appeal, "proof.png", "image/png", new byte[]{9, 8, 7});

        mockMvc.perform(get("/rest/violations/appeal/{id}", evidence.getId()).with(auth(user)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[]{9, 8, 7}));
        mockMvc.perform(get("/rest/violations/appeal/{id}", evidence.getId()).with(auth(officer)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/rest/violations/appeal/{id}", evidence.getId()).with(auth(other)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/rest/violations/appeal/999999").with(auth(user)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listAppealEvidence_permissions() throws Exception {
        Appeal appeal = data.appeal(user, violation);
        data.evidence(appeal, "proof.png", "image/png", new byte[]{1});

        mockMvc.perform(get("/rest/violations/appeal/{id}/evidence", appeal.getId()).with(auth(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/rest/violations/appeal/{id}/evidence", appeal.getId()).with(auth(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/rest/violations/appeal/{id}/evidence", appeal.getId()).with(auth(other)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/rest/violations/appeal/999999/evidence").with(auth(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAppeal_adminOnly() throws Exception {
        Appeal appeal = data.appeal(user, violation);
        String body = """
                {"id": %d, "title": "New title", "description": "New desc", "deadline": "2030-01-01", "resolved": true}
                """;

        mockMvc.perform(put("/rest/violations/appeal").with(auth(user)).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted(appeal.getId()))).andExpect(status().isForbidden());
        mockMvc.perform(put("/rest/violations/appeal").with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted(appeal.getId()))).andExpect(status().isOk());
        mockMvc.perform(put("/rest/violations/appeal").with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted(999999L))).andExpect(status().isNotFound());

        Appeal updated = appealRepo.findById(appeal.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(updated.getResolved()).isTrue();
    }

    @Test
    void deleteAppealAndEvidence_adminOnly() throws Exception {
        Appeal appeal = data.appeal(user, violation);
        AppealEvidence evidence = data.evidence(appeal, "proof.png", "image/png", new byte[]{1});
        data.flushAndClear();

        mockMvc.perform(delete("/rest/violations/appeal/evidence/{id}", evidence.getId()).with(auth(user)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/rest/violations/appeal/evidence/{id}", evidence.getId()).with(auth(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/rest/violations/appeal/evidence/{id}", evidence.getId()).with(auth(admin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/rest/violations/appeal/{id}", appeal.getId()).with(auth(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/rest/violations/appeal/{id}", appeal.getId()).with(auth(admin)))
                .andExpect(status().isNotFound());
    }
}

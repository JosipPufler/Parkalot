package hr.algebra.parkalot.integration;

import hr.algebra.parkalot.AbstractIntegrationTest;
import hr.algebra.parkalot.helper.TestData;
import hr.algebra.parkalot.models.dtos.PermitRequestDto;
import hr.algebra.parkalot.models.entities.*;
import hr.algebra.parkalot.models.enums.EmissionClass;
import hr.algebra.parkalot.models.enums.PermitDuration;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.repositories.*;
import hr.algebra.parkalot.services.PayPalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static hr.algebra.parkalot.helper.TestData.auth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Officer scanning (with the AI reader stubbed) and PayPal order verification (capture stubbed). */
class OfficerAndPaymentTest extends AbstractIntegrationTest {
    @Autowired private TestData data;
    @Autowired private ViolationRepo violationRepo;
    @Autowired private ParkingCheckLogRepo logRepo;
    @Autowired private PermitRepo permitRepo;
    @Autowired private PermitArchiveRepo archiveRepo;

    @MockitoSpyBean private PayPalService payPalService;

    private User user;
    private User officer;
    private User admin;
    private Vehicle vehicle;
    private Area area;

    @BeforeEach
    void setUp() {
        user = data.user("user@test.com", Role.USER);
        officer = data.user("officer@test.com", Role.OFFICER);
        admin = data.user("admin@test.com", Role.ADMIN);
        vehicle = data.vehicle("ZG1111AA", EmissionClass.EURO_6, user);
        area = data.area("Centar", 15, 45, 1);
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder scan(double lat, double lon) {
        return multipart("/rest/officer/scan")
                .file(new MockMultipartFile("image", "plate.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                .param("latitude", String.valueOf(lat))
                .param("longitude", String.valueOf(lon));
    }

    // ---- officer scan ----

    @Test
    void scan_vehicleWithPaidActivePermit_isCompliant() throws Exception {
        when(aiService.readLicencePlate(any())).thenReturn("ZG1111AA");
        data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, true);

        mockMvc.perform(scan(45.5, 15.5).with(auth(officer)))
                .andExpect(status().isOk());

        assertThat(logRepo.findAll()).singleElement().satisfies(l -> assertThat(l.getInViolation()).isFalse());
        assertThat(violationRepo.count()).isZero();
    }

    @Test
    void scan_vehicleWithoutPermit_issuesViolation_andArchivesExpiredPermits() throws Exception {
        when(aiService.readLicencePlate(any())).thenReturn("ZG1111AA");
        Permit expired = data.permit(vehicle, area, user, LocalDate.now().minusDays(10), PermitDuration.DAY, true);
        data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, false); // unpaid -> not active
        data.flushAndClear();

        mockMvc.perform(scan(45.5, 15.5).with(auth(admin)))
                .andExpect(status().isOk());

        data.flushAndClear();
        assertThat(violationRepo.findAll()).hasSize(1);
        assertThat(archiveRepo.findPermitArchiveByLicencePlate("ZG1111AA")).hasSize(1);
        assertThat(permitRepo.existsById(expired.getId())).isFalse();
    }

    @Test
    void scan_outsideAnyArea_returnsBadRequest() throws Exception {
        mockMvc.perform(scan(10.0, 10.0).with(auth(officer))).andExpect(status().isBadRequest());
    }

    @Test
    void scan_asUser_isForbidden() throws Exception {
        mockMvc.perform(scan(45.5, 15.5).with(auth(user))).andExpect(status().isForbidden());
    }

    // ---- PayPal verification ----

    private Map<String, Object> capture(String value, String currency) {
        return Map.of("purchase_units", List.of(Map.of("payments", Map.of("captures",
                List.of(Map.of("amount", Map.of("value", value, "currency_code", currency)))))));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder verify(String json) {
        return post("/rest/payment/permit/verify-order")
                .contentType(MediaType.APPLICATION_JSON).content(json).with(auth(user));
    }

    @Test
    void verifyOrder_violationPayment_marksPaid() throws Exception {
        Violation violation = data.violation(officer, "ZG1111AA", area, true);
        violation.setOrderId("V-1");
        violationRepo.saveAndFlush(violation);
        doReturn(capture("8.99", "EUR")).when(payPalService).captureOrder("V-1");

        mockMvc.perform(verify("{\"orderId\": \"V-1\"}")).andExpect(status().isFound());

        assertThat(violationRepo.findByOrderId("V-1").orElseThrow().getPaymentTime()).isNotNull();
    }

    @Test
    void verifyOrder_permitPayment_marksPaid() throws Exception {
        Permit permit = data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, false);
        permit.setPaypalId("P-1");
        permitRepo.saveAndFlush(permit);
        doReturn(capture("15.0", "EUR")).when(payPalService).captureOrder("P-1");

        mockMvc.perform(verify("{\"orderId\": \"P-1\"}")).andExpect(status().isFound());

        assertThat(permitRepo.findByPaypalId("P-1").orElseThrow().getPaymentTime()).isNotNull();
    }

    @Test
    void verifyOrder_unknownOrder_stillRedirects() throws Exception {
        doReturn(capture("1.00", "EUR")).when(payPalService).captureOrder("NONE");

        mockMvc.perform(verify("{\"orderId\": \"NONE\"}")).andExpect(status().isFound());
    }

    @Test
    void verifyOrder_amountMismatch_returnsBadRequest() throws Exception {
        Violation violation = data.violation(officer, "ZG1111AA", area, true);
        violation.setOrderId("V-2");
        violationRepo.saveAndFlush(violation);
        doReturn(capture("1.00", "EUR")).when(payPalService).captureOrder("V-2");

        mockMvc.perform(verify("{\"orderId\": \"V-2\"}")).andExpect(status().isBadRequest());

        Permit permit = data.permit(vehicle, area, user, LocalDate.now(), PermitDuration.DAY, false);
        permit.setPaypalId("P-2");
        permitRepo.saveAndFlush(permit);
        doReturn(capture("1.00", "EUR")).when(payPalService).captureOrder("P-2");

        mockMvc.perform(verify("{\"orderId\": \"P-2\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void verifyOrder_invalidInput() throws Exception {
        mockMvc.perform(verify("{}")).andExpect(status().isBadRequest());
        mockMvc.perform(verify("{\"orderId\": \"\"}")).andExpect(status().isBadRequest());

        doThrow(new IllegalStateException("boom")).when(payPalService).captureOrder("FAIL");
        mockMvc.perform(verify("{\"orderId\": \"FAIL\"}")).andExpect(status().isInternalServerError());

        doReturn(capture("5.00", "USD")).when(payPalService).captureOrder("USD");
        mockMvc.perform(verify("{\"orderId\": \"USD\"}")).andExpect(status().isBadRequest());

        doReturn(capture("abc", "EUR")).when(payPalService).captureOrder("NAN");
        mockMvc.perform(verify("{\"orderId\": \"NAN\"}")).andExpect(status().isInternalServerError());
    }

    @Test
    void verifyOrder_malformedCaptureResponses_returnServerError() throws Exception {
        List<Map<String, Object>> broken = List.of(
                Map.of(),
                Map.of("purchase_units", List.of()),
                Map.of("purchase_units", List.of(Map.of())),
                Map.of("purchase_units", List.of(Map.of("payments", Map.of()))),
                Map.of("purchase_units", List.of(Map.of("payments", Map.of("captures", List.of())))),
                Map.of("purchase_units", List.of(Map.of("payments", Map.of("captures", List.of(Map.of()))))),
                Map.of("purchase_units", List.of(Map.of("payments", Map.of("captures",
                        List.of(Map.of("amount", Map.of("currency_code", "EUR")))))))
        );
        for (Map<String, Object> response : broken) {
            doReturn(response).when(payPalService).captureOrder("BROKEN");
            mockMvc.perform(verify("{\"orderId\": \"BROKEN\"}")).andExpect(status().isInternalServerError());
        }
    }

    @Test
    void createPermitOrder_forUnknownArea_isEmpty() {
        assertThat(payPalService.createPermitOrder(
                new PermitRequestDto(vehicle.getId(), 999999L, LocalDate.now(), PermitDuration.DAY))).isEmpty();
    }
}

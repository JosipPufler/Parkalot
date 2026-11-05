
package hr.algebra.parkalot.integration;

import hr.algebra.parkalot.models.CustomUserDetails;
import hr.algebra.parkalot.models.dtos.PermitRequestDto;
import hr.algebra.parkalot.models.entities.Area;
import hr.algebra.parkalot.models.entities.Permit;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.models.enums.PermitDuration;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.models.enums.Zone;
import hr.algebra.parkalot.repositories.AreaRepo;
import hr.algebra.parkalot.repositories.PermitRepo;
import hr.algebra.parkalot.repositories.UserRepo;
import hr.algebra.parkalot.repositories.VehicleRepo;
import hr.algebra.parkalot.services.PayPalService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PermitControllerTest {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PermitRepo permitRepository;

    @Autowired
    private VehicleRepo vehicleRepository;

    @Autowired
    private AreaRepo areaRepository;

    @Autowired
    private UserRepo userRepository;

    @MockitoBean
    private PayPalService payPalService;

    private User testUser;
    private User otherUser;
    private User adminUser;
    private User officerUser;

    private Vehicle testVehicle;
    private Vehicle otherVehicle;

    private Area testArea;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        testUser = createUser("user@test.com", Role.USER);
        otherUser = createUser("other@test.com", Role.USER);
        adminUser = createUser("admin@test.com", Role.ADMIN);
        officerUser = createUser("officer@test.com", Role.OFFICER);

        testVehicle = createVehicle("ZG1234AB");
        otherVehicle = createVehicle("ZG5678CD");

        testUser.addVehicle(testVehicle);
        otherUser.addVehicle(otherVehicle);

        testUser = userRepository.saveAndFlush(testUser);
        otherUser = userRepository.saveAndFlush(otherUser);
        adminUser = userRepository.saveAndFlush(adminUser);
        officerUser = userRepository.saveAndFlush(officerUser);

        testVehicle = vehicleRepository.findByLicencePlate("ZG1234AB").orElseThrow();
        otherVehicle = vehicleRepository.findByLicencePlate("ZG5678CD").orElseThrow();

        testArea = areaRepository.saveAndFlush(createArea("Area1"));
    }

    // ---------------------------------------------------------
    // Test data helpers
    // ---------------------------------------------------------

    private User createUser(String email, Role role) {
        return User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password("test-password")
                .role(role)
                .build();
    }

    private Vehicle createVehicle(String licencePlate) {
        return vehicleRepository.save(
                Vehicle.builder()
                        .make("Volkswagen")
                        .model("Golf")
                        .year(2024)
                        .licencePlate(licencePlate)
                        .build()
        );
    }

    private Area createArea(String name) {
        Coordinate[] coordinates = {
                new Coordinate(0, 0),
                new Coordinate(0, 1),
                new Coordinate(1, 1),
                new Coordinate(1, 0),
                new Coordinate(0, 0)
        };

        Polygon polygon = GEOMETRY_FACTORY.createPolygon(coordinates);

        return Area.builder()
                .name(name)
                .areaPolygon(polygon)
                .zone(Zone.ZONE_1)
                .build();
    }

    private Permit createPermit(
            Vehicle vehicle,
            Area area,
            User buyer,
            LocalDate startDate
    ) {
        Permit permit = Permit.builder()
                .vehicle(vehicle)
                .area(area)
                .buyer(buyer)
                .startDate(startDate)
                .permitDuration(PermitDuration.DAY)
                .price(10.0)
                .createdAt(LocalDateTime.now())
                .paypalId("TEST-ORDER-" + System.nanoTime())
                .build();

        return permitRepository.saveAndFlush(permit);
    }

    private RequestPostProcessor authenticatedAs(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        return authentication(auth);
    }

    private String permitRequestJson(Long vehicleId, Long areaId) {
        return """
                {
                    "vehicleId": %d,
                    "areaId": %d,
                    "startDate": "%s",
                    "permitDuration": "DAY"
                }
                """.formatted(vehicleId, areaId, LocalDate.now());
    }

    // ---------------------------------------------------------
    // GET /rest/permits/{licencePlate}
    // ---------------------------------------------------------

    @Test
    void getPermitsForVehicle_whenVehicleBelongsToUser_returnsPermits()
            throws Exception {

        createPermit(testVehicle, testArea, testUser, LocalDate.now());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/rest/permits/{plate}", "ZG1234AB")
                        .with(authenticatedAs(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPermitsForVehicle_whenVehicleDoesNotExist_returnsNotFound()
            throws Exception {

        mockMvc.perform(get("/rest/permits/UNKNOWN")
                        .with(authenticatedAs(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPermitsForVehicle_whenVehicleBelongsToAnotherUser_returnsNotFound()
            throws Exception {

        mockMvc.perform(get("/rest/permits/{plate}", "ZG5678CD")
                        .with(authenticatedAs(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPermitsForVehicle_whenOfficerRequests_returnsPermits()
            throws Exception {

        createPermit(testVehicle, testArea, testUser, LocalDate.now());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/rest/permits/{plate}", "ZG1234AB")
                        .with(authenticatedAs(officerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPermitsForVehicle_whenAdminRequests_returnsPermits()
            throws Exception {

        createPermit(testVehicle, testArea, testUser, LocalDate.now());

        mockMvc.perform(get("/rest/permits/{plate}", "ZG1234AB")
                        .with(authenticatedAs(adminUser)))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------
    // GET /rest/permits
    // ---------------------------------------------------------

    @Test
    void getPermits_whenAuthenticated_returnsAllUserPermits() throws Exception {

        createPermit(testVehicle, testArea, testUser, LocalDate.now());

        entityManager.flush();
        entityManager.clear();

        User savedUser = userRepository.findById(testUser.getId())
                .orElseThrow();

        assertThat(vehicleRepository.findByOwnersContaining(savedUser))
                .hasSize(1);

        assertThat(savedUser.getVehicles()).hasSize(1);

        assertThat(savedUser.getVehicles().getFirst().getPermits())
                .hasSize(1);

        mockMvc.perform(get("/rest/permits")
                        .with(authenticatedAs(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPermits_whenUserHasNoPermits_returnsEmptyArray()
            throws Exception {

        mockMvc.perform(get("/rest/permits")
                        .with(authenticatedAs(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPermits_whenOfficerRequests_returnsForbidden()
            throws Exception {

        mockMvc.perform(get("/rest/permits")
                        .with(authenticatedAs(officerUser)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------
    // POST /rest/permits
    // ---------------------------------------------------------

    @Test
    void requestPermit_whenRequestIsValid_createsPermitAndPaypalOrder()
            throws Exception {

        when(payPalService.createPermitOrder(any(PermitRequestDto.class)))
                .thenReturn(Optional.of("ORDER-123"));

        mockMvc.perform(post("/rest/permits")
                        .with(authenticatedAs(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permitRequestJson(testVehicle.getId(), testArea.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER-123"));

        Permit savedPermit = permitRepository.findByPaypalId("ORDER-123")
                .orElseThrow();

        assertEquals(testVehicle.getId(), savedPermit.getVehicle().getId());
        assertEquals(testArea.getId(), savedPermit.getArea().getId());
        assertEquals(testUser.getId(), savedPermit.getBuyer().getId());
        assertEquals(LocalDate.now(), savedPermit.getStartDate());
        assertEquals(PermitDuration.DAY, savedPermit.getPermitDuration());
        assertNull(savedPermit.getPaymentTime());
    }

    @Test
    void requestPermit_whenVehicleDoesNotExist_returnsNotFound()
            throws Exception {

        mockMvc.perform(post("/rest/permits")
                        .with(authenticatedAs(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permitRequestJson(999999L, testArea.getId())))
                .andExpect(status().isNotFound());

        assertEquals(0, permitRepository.count());
    }

    @Test
    void requestPermit_whenVehicleBelongsToAnotherUser_returnsNotFound()
            throws Exception {

        mockMvc.perform(post("/rest/permits")
                        .with(authenticatedAs(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permitRequestJson(otherVehicle.getId(), testArea.getId())))
                .andExpect(status().isNotFound());

        assertEquals(0, permitRepository.count());
    }

    @Test
    void requestPermit_whenAreaDoesNotExist_returnsNotFound()
            throws Exception {

        mockMvc.perform(post("/rest/permits")
                        .with(authenticatedAs(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permitRequestJson(testVehicle.getId(), 999999L)))
                .andExpect(status().isNotFound());

        assertEquals(0, permitRepository.count());
    }

    @Test
    void requestPermit_whenPermitConflicts_returnsBadRequest()
            throws Exception {

        createPermit(testVehicle, testArea, testUser, LocalDate.now());

        when(payPalService.createPermitOrder(any(PermitRequestDto.class)))
                .thenReturn(Optional.of("ORDER-123"));

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/rest/permits")
                        .with(authenticatedAs(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permitRequestJson(testVehicle.getId(), testArea.getId())))
                .andExpect(status().isBadRequest());

        assertEquals(1, permitRepository.count());
        assertTrue(permitRepository.findByPaypalId("ORDER-123").isEmpty());
    }

    @Test
    void requestPermit_whenPaypalOrderCreationFails_returnsBadRequest()
            throws Exception {

        when(payPalService.createPermitOrder(any(PermitRequestDto.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/rest/permits")
                        .with(authenticatedAs(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permitRequestJson(testVehicle.getId(), testArea.getId())))
                .andExpect(status().isBadRequest());

        assertEquals(0, permitRepository.count());
    }

    @Test
    void requestPermit_whenOfficerRequests_returnsForbidden()
            throws Exception {

        mockMvc.perform(post("/rest/permits")
                        .with(authenticatedAs(officerUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permitRequestJson(testVehicle.getId(), testArea.getId())))
                .andExpect(status().isForbidden());

        assertEquals(0, permitRepository.count());
    }

    // ---------------------------------------------------------
    // PUT /rest/permits
    // ---------------------------------------------------------

    @Test
    void updatePermit_whenAdminRequests_updatesPermit()
            throws Exception {

        Permit permit = createPermit(
                testVehicle,
                testArea,
                testUser,
                LocalDate.now()
        );

        LocalDate newStartDate = LocalDate.now().plusDays(2);

        String requestJson = """
                {
                    "id": %d,
                    "startDate": "%s",
                    "permitDuration": "WEEK",
                    "areaId": %d,
                    "vehicleId": %d
                }
                """.formatted(
                permit.getId(),
                newStartDate,
                testArea.getId(),
                testVehicle.getId()
        );

        mockMvc.perform(put("/rest/permits")
                        .with(authenticatedAs(adminUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        Permit updatedPermit = permitRepository.findById(permit.getId())
                .orElseThrow();

        assertEquals(newStartDate, updatedPermit.getStartDate());
        assertEquals(PermitDuration.WEEK, updatedPermit.getPermitDuration());
        assertEquals(testArea.getId(), updatedPermit.getArea().getId());
        assertEquals(testVehicle.getId(), updatedPermit.getVehicle().getId());
    }

    @Test
    void updatePermit_whenUserRequests_returnsForbidden()
            throws Exception {

        Permit permit = createPermit(
                testVehicle,
                testArea,
                testUser,
                LocalDate.now()
        );

        String requestJson = """
                {
                    "id": %d,
                    "startDate": "%s",
                    "permitDuration": "WEEK",
                    "areaId": %d,
                    "vehicleId": %d
                }
                """.formatted(
                permit.getId(),
                LocalDate.now().plusDays(2),
                testArea.getId(),
                testVehicle.getId()
        );

        mockMvc.perform(put("/rest/permits")
                        .with(authenticatedAs(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());

        Permit unchangedPermit = permitRepository.findById(permit.getId())
                .orElseThrow();

        assertEquals(PermitDuration.DAY, unchangedPermit.getPermitDuration());
    }

    // ---------------------------------------------------------
    // DELETE /rest/permits/{id}
    // ---------------------------------------------------------

    @Test
    void deletePermit_whenPermitExists_deletesPermit()
            throws Exception {

        Permit permit = createPermit(
                testVehicle,
                testArea,
                testUser,
                LocalDate.now()
        );

        mockMvc.perform(delete("/rest/permits/{id}", permit.getId())
                        .with(authenticatedAs(adminUser))
                        .with(csrf()))
                .andExpect(status().isOk());

        assertFalse(permitRepository.findById(permit.getId()).isPresent());
    }

    @Test
    void deletePermit_whenPermitDoesNotExist_returnsNotFound()
            throws Exception {

        mockMvc.perform(delete("/rest/permits/999999")
                        .with(authenticatedAs(adminUser))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePermit_whenUserRequests_returnsForbidden()
            throws Exception {

        Permit permit = createPermit(
                testVehicle,
                testArea,
                testUser,
                LocalDate.now()
        );

        mockMvc.perform(delete("/rest/permits/{id}", permit.getId())
                        .with(authenticatedAs(testUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertTrue(permitRepository.findById(permit.getId()).isPresent());
    }
}

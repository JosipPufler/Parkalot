package hr.algebra.parkalot.integration;

import hr.algebra.parkalot.AbstractIntegrationTest;
import hr.algebra.parkalot.configs.GeometryFactoryProvider;
import hr.algebra.parkalot.helper.TestData;
import hr.algebra.parkalot.models.entities.Household;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.repositories.HouseholdRepo;
import hr.algebra.parkalot.repositories.UserRepo;
import hr.algebra.parkalot.services.GeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static hr.algebra.parkalot.helper.TestData.auth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends AbstractIntegrationTest {
    @Autowired private TestData data;
    @Autowired private UserRepo userRepo;
    @Autowired private HouseholdRepo householdRepo;

    /** Never call the real geocoding provider. */
    @MockitoBean private GeocodingService geocodingService;

    private Household household;
    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        when(geocodingService.geocode(any())).thenReturn(
                GeometryFactoryProvider.geometryFactory.createPoint(new Coordinate(16.0, 45.8)));
        household = data.household("Main street", 15.5, 45.5);
        user = data.userInHousehold("user@test.com", Role.USER, household);
        admin = data.userInHousehold("admin@test.com", Role.ADMIN, household);
    }

    private String userJson(User target, String email, String street) {
        return """
                {"id": %d, "firstName": "New", "lastName": "Name", "email": "%s", "role": "OFFICER",
                 "household": {"id": %d, "street": "%s", "houseNumber": "1", "city": "Zagreb",
                                "postalCode": "10000", "country": "Croatia"}}
                """.formatted(target.getId(), email, household.getId(), street);
    }

    // ---- REST ----

    @Test
    void getUsers_adminOnly() throws Exception {
        mockMvc.perform(get("/rest/users").with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/rest/users").with(auth(user))).andExpect(status().isForbidden());
    }

    @Test
    void updateUser_sameHousehold_updatesFields() throws Exception {
        mockMvc.perform(put("/rest/users").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(user, "renamed@test.com", "Main street")))
                .andExpect(status().isOk());

        User updated = userRepo.findById(user.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("renamed@test.com");
        assertThat(updated.getRole()).isEqualTo(Role.OFFICER);
        assertThat(updated.getHousehold().getId()).isEqualTo(household.getId());
    }

    @Test
    void updateUser_newAddress_createsGeocodedHousehold() throws Exception {
        mockMvc.perform(put("/rest/users").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(user, user.getEmail(), "Brand new street")))
                .andExpect(status().isOk());

        User updated = userRepo.findById(user.getId()).orElseThrow();
        assertThat(updated.getHousehold().getStreet()).isEqualTo("Brand new street");
        assertThat(updated.getHousehold().getId()).isNotEqualTo(household.getId());
    }

    @Test
    void updateUser_addressOfExistingHousehold_reusesIt() throws Exception {
        Household existing = data.household("Existing street", 15.6, 45.6);

        mockMvc.perform(put("/rest/users").with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(user, user.getEmail(), "Existing street")))
                .andExpect(status().isOk());

        assertThat(userRepo.findById(user.getId()).orElseThrow().getHousehold().getId()).isEqualTo(existing.getId());
    }

    @Test
    void updateUser_errors() throws Exception {
        User ghost = User.builder().id(999999L).build();
        mockMvc.perform(put("/rest/users").with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(userJson(ghost, "x@test.com", "Main street"))).andExpect(status().isNotFound());

        // unknown household
        mockMvc.perform(put("/rest/users").with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(userJson(user, user.getEmail(), "Main street").replace(
                        "\"id\": " + household.getId(), "\"id\": 999999"))).andExpect(status().isNotFound());

        // e-mail already taken -> conflict
        mockMvc.perform(put("/rest/users").with(auth(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(userJson(user, admin.getEmail(), "Main street"))).andExpect(status().isConflict());
    }

    @Test
    void deleteUser_adminOnly() throws Exception {
        User victim = data.userInHousehold("victim@test.com", Role.USER, household);
        data.flushAndClear();

        mockMvc.perform(delete("/rest/users/{id}", victim.getId()).with(auth(user))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/rest/users/{id}", victim.getId()).with(auth(admin))).andExpect(status().isOk());
        data.flushAndClear();
        assertThat(userRepo.existsById(victim.getId())).isFalse();
        mockMvc.perform(delete("/rest/users/{id}", victim.getId()).with(auth(admin))).andExpect(status().isNotFound());
    }

    // ---- MVC registration ----

    @Test
    void registerPage_isPublic() throws Exception {
        mockMvc.perform(get("/mvc/public/auth/register"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("registrationForm"))
                .andExpect(view().name("register"));
    }

    @Test
    void loginPage_showsErrorAndLogoutFlags() throws Exception {
        mockMvc.perform(get("/mvc/public/auth/login"))
                .andExpect(status().isOk()).andExpect(view().name("login"));
        mockMvc.perform(get("/mvc/public/auth/login").param("error", "").param("logout", ""))
                .andExpect(model().attribute("loginError", true))
                .andExpect(model().attribute("logoutSuccess", true));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registration(
            String email, String confirm) {
        return post("/mvc/public/auth/register").with(csrf())
                .param("firstName", "Joe").param("lastName", "Schmoe")
                .param("password", "pass").param("confirmPassword", confirm)
                .param("email", email)
                .param("household.street", "Ilica").param("household.houseNumber", "5")
                .param("household.city", "Zagreb").param("household.postalCode", "10000")
                .param("household.country", "Croatia");
    }

    @Test
    void register_validForm_createsUserAndRedirects() throws Exception {
        mockMvc.perform(registration("joe@test.com", "pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/mvc/public/auth/login?registered"));

        User created = userRepo.findByEmail("joe@test.com").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.USER);
        assertThat(created.getHousehold().getStreet()).isEqualTo("Ilica");
        assertThat(created.getPassword()).isNotEqualTo("pass");
    }

    @Test
    void register_secondUserAtSameAddress_sharesHousehold() throws Exception {
        mockMvc.perform(registration("joe@test.com", "pass"));
        mockMvc.perform(registration("jane@test.com", "pass")).andExpect(status().is3xxRedirection());

        assertThat(userRepo.findByEmail("joe@test.com").orElseThrow().getHousehold().getId())
                .isEqualTo(userRepo.findByEmail("jane@test.com").orElseThrow().getHousehold().getId());
        assertThat(householdRepo.findAll().stream().filter(h -> "Ilica".equals(h.getStreet()))).hasSize(1);
    }

    @Test
    void register_passwordMismatch_redisplaysForm() throws Exception {
        mockMvc.perform(registration("joe@test.com", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
        assertThat(userRepo.findByEmail("joe@test.com")).isEmpty();
    }
}

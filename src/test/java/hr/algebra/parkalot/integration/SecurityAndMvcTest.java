package hr.algebra.parkalot.integration;

import hr.algebra.parkalot.AbstractIntegrationTest;
import hr.algebra.parkalot.helper.TestData;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.services.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static hr.algebra.parkalot.helper.TestData.auth;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Authentication (JWT cookie, form login/logout) and the Thymeleaf page controllers. */
class SecurityAndMvcTest extends AbstractIntegrationTest {
    @Autowired private TestData data;
    @Autowired private JwtService jwtService;

    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        user = data.user("user@test.com", Role.USER);
        admin = data.user("admin@test.com", Role.ADMIN);
    }

    private static User ghost() {
        return User.builder().id(987654L).email("ghost@test.com").password("x").role(Role.USER).build();
    }

    // ---- JWT cookie on the REST chain ----

    @Test
    void rest_withoutCredentials_returnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/rest/area"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void rest_withValidJwtCookie_isAuthenticated() throws Exception {
        mockMvc.perform(get("/rest/area").cookie(new Cookie("JWT_TOKEN", jwtService.generateJwtToken(user))))
                .andExpect(status().isOk());
    }

    @Test
    void rest_withGarbageJwtCookie_isUnauthorized() throws Exception {
        mockMvc.perform(get("/rest/area").cookie(new Cookie("JWT_TOKEN", "not-a-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rest_withJwtOfUnknownUser_isUnauthorized() throws Exception {
        mockMvc.perform(get("/rest/area").cookie(new Cookie("JWT_TOKEN", jwtService.generateJwtToken(ghost()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rest_withOtherCookieOnly_isUnauthorized() throws Exception {
        mockMvc.perform(get("/rest/area").cookie(new Cookie("OTHER", "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rest_publicPathsSkipTokenFilter() throws Exception {
        mockMvc.perform(get("/rest/public/anything"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    // ---- form login / logout ----

    @Test
    void formLogin_withValidCredentials_setsJwtCookieAndRedirectsToMap() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "user@test.com").param("password", TestData.PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mvc/map"))
                .andExpect(header().string("Set-Cookie", containsString("JWT_TOKEN=")));
    }

    @Test
    void formLogin_withWrongPassword_redirectsBackToLogin() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("email", "user@test.com").param("password", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mvc/public/auth/login?error"));
    }

    @Test
    void logout_clearsJwtCookie() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    // ---- page controllers ----

    @Test
    void pages_renderForAuthenticatedUsers() throws Exception {
        for (String page : new String[]{"map", "areas", "violations", "permits", "appeals"}) {
            mockMvc.perform(get("/mvc/" + page).with(auth(user)))
                    .andExpect(status().isOk())
                    .andExpect(view().name(page))
                    .andExpect(model().attribute("userRole", "USER"))
                    .andExpect(model().attributeExists("paypalClientId"));
        }
    }

    @Test
    void adminPages_requireAdminRole() throws Exception {
        mockMvc.perform(get("/mvc/admin/users").with(auth(admin))).andExpect(status().isOk()).andExpect(view().name("admin/users"));
        mockMvc.perform(get("/mvc/admin/vehicles").with(auth(admin))).andExpect(status().isOk()).andExpect(view().name("admin/vehicles"));
        mockMvc.perform(get("/mvc/admin/users").with(auth(user))).andExpect(status().isForbidden());
        mockMvc.perform(get("/mvc/admin/vehicles").with(auth(user))).andExpect(status().isForbidden());
    }

    @Test
    void pages_withoutLogin_redirectToLoginPage() throws Exception {
        mockMvc.perform(get("/mvc/map"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mvc/public/auth/login"));
    }

    @Test
    void pages_whenAuthenticatedUserNoLongerExists_redirectToLogin() throws Exception {
        mockMvc.perform(get("/mvc/map").with(auth(ghost())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mvc/public/auth/login"));
    }
}

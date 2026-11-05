package hr.algebra.parkalot.integration;

import hr.algebra.parkalot.models.dtos.RegistrationForm;
import hr.algebra.parkalot.repositories.UserRepo;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    private UserRepo userRepo;

    @Test
    void shouldShowRegisterPage() throws Exception {
        mvc.perform(get("/mvc/public/auth/register"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("registrationForm"))
                .andExpect(view().name("register"));
    }

    @Test
    void shouldRegisterUser() throws Exception {
        RegistrationForm userRegisterForm = new RegistrationForm("Joe", "Schmoe", "pass", "pass", "email@mail.com");

        mvc.perform(post("/mvc/public/auth/register")
                        .with(csrf())
                        .param("firstName", userRegisterForm.firstName())
                        .param("firstName", userRegisterForm.lastName())
                        .param("password", userRegisterForm.password())
                        .param("confirmPassword", userRegisterForm.confirmPassword())
                        .param("email", userRegisterForm.email()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/mvc/public/auth/login?registered"));

        assertEquals(1, userRepo.count());
    }
}

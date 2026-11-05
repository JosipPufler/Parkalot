package hr.algebra.parkalot.controllers.mvc;

import hr.algebra.parkalot.models.dtos.RegistrationForm;
import hr.algebra.parkalot.services.db.UserService;
import hr.algebra.parkalot.services.utils.Mapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/mvc/public/auth")
@RequiredArgsConstructor
public class UserMvcController {
    private final UserService userService;

    private static final String REGISTER_PAGE = "register";
    private static final String PROFILE_PAGE = "profile";
    private static final String LOGIN_PAGE = "login";
    private static final String ERROR_USER_DTO = "error.userDto";

    private static final String MODEL_ATTRIBUTE_REGISTRATION_FORM = "registrationForm";

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value="error", required=false) String error,
            @RequestParam(value="logout", required=false) String logout,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        if (logout != null) {
            model.addAttribute("logoutSuccess", true);
        }
        return LOGIN_PAGE;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_REGISTRATION_FORM, new RegistrationForm());
        return REGISTER_PAGE;
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registrationForm") @Valid RegistrationForm registrationForm, BindingResult result) {
        if (!registrationForm.password().equals(registrationForm.confirmPassword())) {
            result.rejectValue("confirmPassword", ERROR_USER_DTO, "Passwords do not match.");
        }

        if (userService.existsByEmail(registrationForm.email())) {
            result.rejectValue("username", ERROR_USER_DTO, "Username already exists.");
        }

        if (result.hasErrors()) {
            return REGISTER_PAGE;
        }

        userService.save(Mapper.mapRegistrationFormToUser(registrationForm));
        return "redirect:/mvc/public/auth/login?registered";
    }
}

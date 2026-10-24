package hr.algebra.parkalot.controllers.mvc;

import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.services.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/mvc")
@RequiredArgsConstructor
public class MainMvcController {
    private static final String AREA_PAGE_NAME = "areas";
    private static final String VIOLATIONS_PAGE_NAME = "violations";
    private static final String PERMITS_PAGE_NAME = "permits";
    private static final String APPEALS_PAGE_NAME = "appeals";

    private final AuthUtils authUtils;

    @GetMapping("/areas")
    public String showAreasPage(Model model){
        Optional<User> currentUser = authUtils.getCurrentUser();

        if (currentUser.isEmpty()) {
            return "redirect:/mvc/public/auth/login";
        }

        model.addAttribute("userRole", currentUser.get().getRole().name());
        return AREA_PAGE_NAME;
    }

    @GetMapping("/violations")
    public String showViolationsPage(Model model){
        Optional<User> currentUser = authUtils.getCurrentUser();

        if (currentUser.isEmpty()) {
            return "redirect:/mvc/public/auth/login";
        }

        model.addAttribute("userRole", currentUser.get().getRole().name());
        return VIOLATIONS_PAGE_NAME;
    }

    @GetMapping("/permits")
    public String showPermitsPage(Model model){
        Optional<User> currentUser = authUtils.getCurrentUser();

        if (currentUser.isEmpty()) {
            return "redirect:/mvc/public/auth/login";
        }

        model.addAttribute("userRole", currentUser.get().getRole().name());
        return PERMITS_PAGE_NAME;
    }

    @GetMapping("/appeals")
    public String showAppealsPage(Model model){
        Optional<User> currentUser = authUtils.getCurrentUser();

        if (currentUser.isEmpty()) {
            return "redirect:/mvc/public/auth/login";
        }

        model.addAttribute("userRole", currentUser.get().getRole().name());
        return APPEALS_PAGE_NAME;
    }
}

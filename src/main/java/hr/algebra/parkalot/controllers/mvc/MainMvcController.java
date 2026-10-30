package hr.algebra.parkalot.controllers.mvc;

import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.services.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.AccessDeniedException;
import java.util.Optional;

@Controller
@RequestMapping("/mvc")
@RequiredArgsConstructor
public class MainMvcController {
    private static final String AREA_PAGE_NAME = "areas";
    private static final String VIOLATIONS_PAGE_NAME = "violations";
    private static final String PERMITS_PAGE_NAME = "permits";
    private static final String APPEALS_PAGE_NAME = "appeals";

    private static final String USER_ROLE_ATTRIBUTE = "userRole";

    private final AuthUtils authUtils;

    @GetMapping("/areas")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public String showAreasPage(Model model){
        User currentUser = authUtils.requireCurrentUser();

        model.addAttribute(USER_ROLE_ATTRIBUTE, currentUser.getRole().name());
        return AREA_PAGE_NAME;
    }

    @GetMapping("/violations")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public String showViolationsPage(Model model){
        User currentUser = authUtils.requireCurrentUser();

        model.addAttribute(USER_ROLE_ATTRIBUTE, currentUser.getRole().name());
        return VIOLATIONS_PAGE_NAME;
    }

    @GetMapping("/permits")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public String showPermitsPage(Model model){
        User currentUser = authUtils.requireCurrentUser();

        model.addAttribute(USER_ROLE_ATTRIBUTE, currentUser.getRole().name());
        return PERMITS_PAGE_NAME;
    }

    @GetMapping("/appeals")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public String showAppealsPage(Model model) {
        User currentUser = authUtils.requireCurrentUser();

        model.addAttribute(USER_ROLE_ATTRIBUTE, currentUser.getRole().name());
        return APPEALS_PAGE_NAME;
    }
}

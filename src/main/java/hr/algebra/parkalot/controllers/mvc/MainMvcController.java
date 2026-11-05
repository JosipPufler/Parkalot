package hr.algebra.parkalot.controllers.mvc;

import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.services.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mvc")
@RequiredArgsConstructor
public class MainMvcController {
    private static final String MAP_PAGE_NAME = "map";
    private static final String AREAS_PAGE_NAME = "areas";
    private static final String VIOLATIONS_PAGE_NAME = "violations";
    private static final String PERMITS_PAGE_NAME = "permits";
    private static final String APPEALS_PAGE_NAME = "appeals";

    private static final String USER_ROLE_ATTRIBUTE = "userRole";
    private static final String PAYPAL_CLIENT_ID_ATTRIBUTE = "paypalClientId";

    @Value("${paypal.clientId}")
    private final String paypalClientId;
    private final AuthUtils authUtils;

    @GetMapping("/map")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public String showMapPage(Model model){
        addUniversalProperties(model);
        return MAP_PAGE_NAME;
    }

    @GetMapping("/areas")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public String showAreasPage(Model model){
        addUniversalProperties(model);
        return AREAS_PAGE_NAME;
    }

    @GetMapping("/violations")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public String showViolationsPage(Model model){
        addUniversalProperties(model);
        return VIOLATIONS_PAGE_NAME;
    }

    @GetMapping("/permits")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public String showPermitsPage(Model model){
        addUniversalProperties(model);
        return PERMITS_PAGE_NAME;
    }

    @GetMapping("/appeals")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public String showAppealsPage(Model model) {
        addUniversalProperties(model);
        return APPEALS_PAGE_NAME;
    }

    private void addUniversalProperties(Model model){
        User currentUser = authUtils.requireCurrentUser();

        model.addAttribute(USER_ROLE_ATTRIBUTE, currentUser.getRole().name());
        model.addAttribute(PAYPAL_CLIENT_ID_ATTRIBUTE, paypalClientId);
    }
}

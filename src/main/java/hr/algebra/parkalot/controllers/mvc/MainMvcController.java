package hr.algebra.parkalot.controllers.mvc;

import hr.algebra.parkalot.models.dto.ViolationDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mvc")
public class MainMvcController {
    private static final String AREA_PAGE_NAME = "areas";
    private static final String VIOLATIONS_PAGE_NAME = "violations";
    private static final String PERMITS_PAGE_NAME = "violations";

    @GetMapping("/areas")
    public String showAreasPage(){
        return AREA_PAGE_NAME;
    }

    @GetMapping("/violations")
    public String showViolationsPage(){
        return VIOLATIONS_PAGE_NAME;
    }

    @GetMapping("/permits")
    public String showPermitsPage(){
        return PERMITS_PAGE_NAME;
    }
}

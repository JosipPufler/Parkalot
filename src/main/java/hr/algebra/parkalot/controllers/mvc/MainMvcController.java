package hr.algebra.parkalot.controllers.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mvc/public")
public class MainMvcController {
    private static final String AREA_PAGE_NAME = "areas";

    @GetMapping("/areas")
    public String showAreasPage(){
        return AREA_PAGE_NAME;
    }
}

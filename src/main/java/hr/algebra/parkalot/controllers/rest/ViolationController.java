package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.dto.AppealDto;
import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Vehicle;
import hr.algebra.parkalot.models.entity.Violation;
import hr.algebra.parkalot.models.dto.ViolationDto;
import hr.algebra.parkalot.services.db.AppealService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import hr.algebra.parkalot.services.db.VehicleService;
import hr.algebra.parkalot.services.db.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/rest/violations")
@RequiredArgsConstructor
public class ViolationController {
    private final AuthUtils authUtils;
    private final ViolationService violationService;
    private final VehicleService vehicleService;
    private final AppealService appealService;

    @GetMapping
    public ResponseEntity<?> getViolations() {
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse("Unauthorized");
        }

        List<Violation> violations = vehicleService.findByUser(currentUser.get()).stream()
                .flatMap(vehicle -> violationService.findViolationsByLicencePlate(vehicle.getLicencePlate()).stream()).toList();
        return ResponseEntity.ok(violations.stream().map(ViolationDto::new));
    }

    @GetMapping("/vehicle")
    public ResponseEntity<?> getVehicleViolations(@RequestParam String licensePlate) {
        return ResponseEntity.ok(violationService.findViolationsByLicencePlate(licensePlate).stream().map(ViolationDto::new));
    }

    @PostMapping()
    public ResponseEntity<?> resolveViolation(@RequestParam Long violationId){
        Optional<Violation> byId = violationService.findById(violationId);
        if (byId.isEmpty()) {
            return HttpUtils.produceNotFoundResponse("Violation not found");
        }

        Violation violation = byId.get();
        if (violation.getActive()) {
            violation.setActive(false);
            violationService.save(violation);
            return ResponseEntity.ok().build();
        }
        return HttpUtils.produceBadRequestResponse("Violation already resolved");
    }
}

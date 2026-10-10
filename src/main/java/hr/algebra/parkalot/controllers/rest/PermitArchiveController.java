package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Vehicle;
import hr.algebra.parkalot.models.dto.PermitArchiveDto;
import hr.algebra.parkalot.services.db.PermitArchiveService;
import hr.algebra.parkalot.services.db.VehicleService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/rest/permits/past")
@RequiredArgsConstructor
public class PermitArchiveController {
    private final PermitArchiveService permitArchiveService;
    private final VehicleService vehicleService;
    private final AuthUtils authUtils;

    @GetMapping("/{licencePlate}")
    public ResponseEntity<?> getPastPermitsForVehicle(@PathVariable String licencePlate) {
        Optional<Vehicle> vehicle = vehicleService.findByLicencePlate(licencePlate);
        Optional<User> currentUser = authUtils.getCurrentUser();

        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse("Unauthorized");
        }

        if (vehicle.isPresent() && vehicle.get().getOwner().getId().equals(currentUser.get().getId())) {
            return ResponseEntity.ok(permitArchiveService.findByLicencePlate(vehicle.get().getLicencePlate())
                    .stream().map(PermitArchiveDto::new).toList());
        }
        return HttpUtils.produceNotFoundResponse("This vehicle is not registered to you.");
    }

    @GetMapping()
    public ResponseEntity<?> getPastPermits() {
        Optional<User> currentUser = authUtils.getCurrentUser();

        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse("Unauthorized");
        }

        return ResponseEntity.ok(currentUser.get().getVehicles().stream()
                .flatMap(x -> permitArchiveService.findByLicencePlate(x.getLicencePlate())
                        .stream().map(PermitArchiveDto::new)).toList());
    }
}

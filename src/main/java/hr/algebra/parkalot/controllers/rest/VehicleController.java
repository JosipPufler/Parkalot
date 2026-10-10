package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Vehicle;
import hr.algebra.parkalot.models.dto.PermitArchiveDto;
import hr.algebra.parkalot.models.dto.VehicleDetailsDto;
import hr.algebra.parkalot.models.dto.VehicleDto;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.db.PermitArchiveService;
import hr.algebra.parkalot.services.db.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/rest/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService vehicleService;
    private final PermitArchiveService permitArchiveService;
    private final AuthUtils authUtils;

    @GetMapping
    public ResponseEntity<List<VehicleDto>> getAllVehicles() {
        return authUtils.getCurrentUser().map(user ->
                        ResponseEntity.ok(vehicleService.findByOwner(user).stream().map(VehicleDto::new).toList()))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @GetMapping("/{licencePlate}")
    public ResponseEntity<VehicleDetailsDto> getVehicle(@PathVariable String licencePlate) {
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isPresent()) {
            Optional<Vehicle> byLicencePlate = vehicleService.findByLicencePlate(licencePlate);
            if (byLicencePlate.isPresent()
                    && Objects.equals(byLicencePlate.get().getOwner().getId(), currentUser.get().getId())) {
                return ResponseEntity.ok(new VehicleDetailsDto(byLicencePlate.get(),
                        permitArchiveService.findByLicencePlate(licencePlate).stream().map(PermitArchiveDto::new).toList()));
            }
        }
        return ResponseEntity.status(401).build();
    }
}

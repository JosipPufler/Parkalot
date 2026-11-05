package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.models.dtos.PermitArchiveDto;
import hr.algebra.parkalot.services.db.PermitArchiveService;
import hr.algebra.parkalot.services.db.VehicleService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
        User currentUser = authUtils.requireCurrentUser();

        if (vehicle.isPresent() && vehicle.get().isOwnedBy(currentUser)) {
            return ResponseEntity.ok(permitArchiveService.findByLicencePlate(vehicle.get().getLicencePlate())
                    .stream().map(PermitArchiveDto::new).toList());
        }
        return HttpUtils.produceNotFoundResponse("This vehicleDto is not registered to you.");
    }

    @GetMapping()
    public ResponseEntity<?> getPastPermits() {
        User currentUser = authUtils.requireCurrentUser();

        return ResponseEntity.ok(currentUser.getVehicles().stream()
                .flatMap(x -> permitArchiveService.findByLicencePlate(x.getLicencePlate())
                        .stream().map(PermitArchiveDto::new)).toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deletePermitArchive(@PathVariable Long id){
        if(!permitArchiveService.existsById(id)){
            return HttpUtils.produceNotFoundResponse("No past permit for the given id: " + id);
        }
        permitArchiveService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }
}

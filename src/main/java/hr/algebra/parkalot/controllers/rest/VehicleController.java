package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.dtos.VehicleUpdateForm;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.models.dtos.PermitArchiveDto;
import hr.algebra.parkalot.models.dtos.VehicleDetailsDto;
import hr.algebra.parkalot.models.dtos.VehicleDto;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.db.PermitArchiveService;
import hr.algebra.parkalot.services.db.VehicleService;
import hr.algebra.parkalot.services.utils.HttpUtils;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.io.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/rest/vehicle")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService vehicleService;
    private final PermitArchiveService permitArchiveService;
    private final AuthUtils authUtils;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getAllVehicles() {
        User currentUser = authUtils.requireCurrentUser();

        if (currentUser.getRole() == Role.ADMIN) {
            return HttpUtils.produceOkResponseWithBody(vehicleService.findAll().stream().map(VehicleDto::new).toList());
        }

        List<VehicleDto> vehicles = vehicleService.findByOwner(currentUser).stream().map(VehicleDto::new).toList();
        return HttpUtils.produceOkResponseWithBody(vehicles);
    }

    @GetMapping("/{licencePlate}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getVehicle(@PathVariable String licencePlate) throws ParseException {
        Optional<Vehicle> vehicle = vehicleService.findByLicencePlate(licencePlate);
        if (vehicle.isPresent()
                && vehicle.get().isOwnedBy(authUtils.requireCurrentUser())) {
            return ResponseEntity.ok(new VehicleDetailsDto(vehicle.get(),
                    permitArchiveService.findByLicencePlate(licencePlate).stream().map(PermitArchiveDto::new).toList()));
        }
        return HttpUtils.produceNotFoundResponse("Vehicle not found");
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> createVehicle(@RequestBody VehicleDto vehicleDto) {
        Vehicle vehicle = vehicleService.createVehicleFromDto(vehicleDto, authUtils.requireCurrentUser());
        return HttpUtils.produceOkResponseWithBody(new VehicleDto(vehicle));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id){
        if(!vehicleService.existsById(id)){
            return HttpUtils.produceNotFoundResponse("No vehicleDto for the given id: " + id);
        }
        vehicleService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }

    @PutMapping()
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> updateVehicle(@RequestBody VehicleUpdateForm vehicleDto) {
        return HttpUtils.produceOkResponseWithBody(new VehicleDto(vehicleService.updateVehicle(vehicleDto)));
    }
}

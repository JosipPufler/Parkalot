package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Vehicle;
import hr.algebra.parkalot.models.dto.PermitArchiveDto;
import hr.algebra.parkalot.models.dto.VehicleDetailsDto;
import hr.algebra.parkalot.models.dto.VehicleDto;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.db.PermitArchiveService;
import hr.algebra.parkalot.services.db.VehicleService;
import hr.algebra.parkalot.services.utils.HttpUtils;
import hr.algebra.parkalot.services.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.io.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
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

        return ResponseEntity.ok(vehicleService.findByOwner(currentUser).stream().map(VehicleDto::new).toList());
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
        Vehicle vehicle = Mapper.mapVehicleDtoToVehicle(vehicleDto, authUtils.requireCurrentUser());
        return ResponseEntity.ok(new VehicleDto(vehicleService.save(vehicle)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id){
        if(!vehicleService.existsById(id)){
            return HttpUtils.produceNotFoundResponse("No vehicle for the given id: " + id);
        }
        vehicleService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }
}

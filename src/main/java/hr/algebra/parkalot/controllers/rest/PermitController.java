package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.dto.PermitRequestDto;
import hr.algebra.parkalot.models.entity.Area;
import hr.algebra.parkalot.models.entity.Permit;
import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Vehicle;
import hr.algebra.parkalot.models.dto.PermitDto;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import hr.algebra.parkalot.services.db.PermitService;
import hr.algebra.parkalot.services.db.VehicleService;
import hr.algebra.parkalot.services.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.io.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/rest/permits")
@RequiredArgsConstructor
public class PermitController {
    private final PermitService permitService;
    private final VehicleService vehicleService;
    private final AreaService areaService;
    private final AuthUtils authUtils;

    @GetMapping("/{licencePlate}")
    public ResponseEntity<?> getPermitsForVehicle(@PathVariable String licencePlate) throws ParseException {
        Optional<Vehicle> byLicencePlate = vehicleService.findByLicencePlate(licencePlate);
        Optional<User> currentUser = authUtils.getCurrentUser();

        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse("Unauthorized");
        }

        if (byLicencePlate.isPresent() && byLicencePlate.get().getOwner().getId().equals(currentUser.get().getId())) {
            List<PermitDto> list = new ArrayList<>();
            for (Permit permit : byLicencePlate.get().getPermits()) {
                PermitDto permitDto = new PermitDto(permit);
                list.add(permitDto);
            }
            return ResponseEntity.ok(list);
        }
        return HttpUtils.produceNotFoundResponse("This vehicle is not registered to you.");
    }

    @GetMapping()
    public ResponseEntity<?> getPermits() throws ParseException {
        Optional<User> currentUser = authUtils.getCurrentUser();

        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse("Unauthorized");
        }

        List<PermitDto> list = new ArrayList<>();
        for (Vehicle x : currentUser.get().getVehicles()) {
            for (Permit permit : x.getPermits()) {
                PermitDto permitDto = new PermitDto(permit);
                list.add(permitDto);
            }
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping()
    public ResponseEntity<?> requestPermit(@RequestBody PermitRequestDto permitRequest) throws ParseException {
        Optional<User> currentUser = authUtils.getCurrentUser();

        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse("Unauthorized");
        }

        Optional<Vehicle> vehicle = vehicleService.findById(permitRequest.vehicleId());
        if (vehicle.isEmpty() || !vehicle.get().getOwner().getId().equals(currentUser.get().getId())) {
            return HttpUtils.produceNotFoundResponse("Vehicle is not registered to you.");
        }

        Optional<Area> area = areaService.findById(permitRequest.areaId());
        if (area.isEmpty()) {
            return HttpUtils.produceNotFoundResponse("Area not found.");
        }

        Optional<Permit> permit = permitService.tryBuyPermit(Mapper.mapPermitRequestToPermit(permitRequest, vehicle.get(), area.get()));
        return permit.isPresent() ? ResponseEntity.ok(new PermitDto(permit.get()))
                : HttpUtils.produceBadRequestResponse("Permit conflicts with already purchased permit.");
    }
}

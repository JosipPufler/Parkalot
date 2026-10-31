package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.Role;
import hr.algebra.parkalot.models.dto.PermitRequestDto;
import hr.algebra.parkalot.models.entity.Area;
import hr.algebra.parkalot.models.entity.Permit;
import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Vehicle;
import hr.algebra.parkalot.models.dto.PermitDto;
import hr.algebra.parkalot.services.PayPalService;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import hr.algebra.parkalot.services.db.PermitService;
import hr.algebra.parkalot.services.db.VehicleService;
import hr.algebra.parkalot.services.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.io.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.*;

@RestController
@RequestMapping("/rest/permits")
@RequiredArgsConstructor
public class PermitController {
    private final PermitService permitService;
    private final VehicleService vehicleService;
    private final AreaService areaService;
    private final AuthUtils authUtils;
    private final PayPalService payPalService;

    @GetMapping("/{licencePlate}")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> getPermitsForVehicle(@PathVariable String licencePlate) throws ParseException {
        Optional<Vehicle> vehicle = vehicleService.findByLicencePlate(licencePlate);
        User currentUser = authUtils.requireCurrentUser();

        if (vehicle.isPresent()
                && (vehicle.get().isOwnedBy(currentUser)
                    || currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.OFFICER)) {
            List<PermitDto> list = new ArrayList<>();
            for (Permit permit : vehicle.get().getPermits()) {
                PermitDto permitDto = new PermitDto(permit);
                list.add(permitDto);
            }
            return ResponseEntity.ok(list);
        }
        return HttpUtils.produceNotFoundResponse("This vehicle is not registered to you.");
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getPermits() throws ParseException {
        User currentUser = authUtils.requireCurrentUser();

        List<PermitDto> list = new ArrayList<>();
        for (Vehicle x : currentUser.getVehicles()) {
            for (Permit permit : x.getPermits()) {
                PermitDto permitDto = new PermitDto(permit);
                list.add(permitDto);
            }
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> requestPermit(@RequestBody PermitRequestDto permitRequest) {
        User currentUser = authUtils.requireCurrentUser();

        Optional<Vehicle> vehicle = vehicleService.findById(permitRequest.vehicleId());
        if (vehicle.isEmpty() || !vehicle.get().isOwnedBy(currentUser)) {
            return HttpUtils.produceNotFoundResponse("Vehicle is not registered to you.");
        }

        Optional<Area> area = areaService.findById(permitRequest.areaId());
        if (area.isEmpty()) {
            return HttpUtils.produceNotFoundResponse("Area not found.");
        }

        Optional<Permit> permitOpt = permitService.tryBuyPermit(Mapper.mapPermitRequestToPermit(permitRequest, vehicle.get(), area.get()));

        if (permitOpt.isEmpty()) {
            return HttpUtils.produceBadRequestResponse("Permit is conflicting.");
        }
        Permit permit = permitOpt.get();
        Optional<String> orderId = payPalService.createPermitOrder(permitRequest);

        if (orderId.isEmpty())
            return HttpUtils.produceBadRequestResponse("Invalid request");

        permit.setPaypalId(orderId.get());
        permitService.save(permit);

        Map<String, String> response = new HashMap<>();
        response.put("orderId", orderId.get());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deletePermit(@PathVariable Long id){
        if(!permitService.existsById(id)){
            return HttpUtils.produceNotFoundResponse("No permit for the given id: " + id);
        }
        permitService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }
}

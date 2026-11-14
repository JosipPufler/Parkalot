package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.exceptions.LocalizedBadRequestException;
import hr.algebra.parkalot.exceptions.LocalizedNotFoundException;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.models.dtos.PermitRequestDto;
import hr.algebra.parkalot.models.dtos.PermitUpdateForm;
import hr.algebra.parkalot.models.entities.Area;
import hr.algebra.parkalot.models.entities.Permit;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.models.dtos.PermitDto;
import hr.algebra.parkalot.services.PayPalService;
import hr.algebra.parkalot.services.PermitEligibilityService;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import hr.algebra.parkalot.services.db.PermitService;
import hr.algebra.parkalot.services.db.VehicleService;
import hr.algebra.parkalot.services.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    private final PermitEligibilityService permitEligibilityService;

    @GetMapping("/{licencePlate}")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> getPermitsForVehicle(@PathVariable String licencePlate) {
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
        throw new LocalizedNotFoundException(LocalizedExceptionKeys.VEHICLE_NOT_OWNED);
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getPermits() {
        User currentUser = authUtils.requireCurrentUser();

        if (currentUser.getRole() == Role.ADMIN) {
            return HttpUtils.produceOkResponseWithBody(permitService
                    .findAll().stream().map(PermitDto::new).toList());
        }

        List<PermitDto> list = new ArrayList<>();
        for (Vehicle x : currentUser.getVehicles()) {
            for (Permit permit : x.getPermits()) {
                PermitDto permitDto = new PermitDto(permit);
                list.add(permitDto);
            }
        }
        return HttpUtils.produceOkResponseWithBody(list);
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> requestPermit(@RequestBody PermitRequestDto permitRequest) {
        User currentUser = authUtils.requireCurrentUser();
        Vehicle vehicle = vehicleService.findById(permitRequest.vehicleId()).orElseThrow(() ->
                new LocalizedNotFoundException(LocalizedExceptionKeys.VEHICLE_NOT_FOUND, permitRequest.vehicleId()));
        Area area = areaService.findById(permitRequest.areaId()).orElseThrow(() ->
                new LocalizedNotFoundException(LocalizedExceptionKeys.AREA_NOT_FOUND, permitRequest.areaId()));

        Permit permit = Mapper.mapPermitRequestToPermit(permitRequest, vehicle, area, currentUser);

        if (permitService.permitsConflict(permit)) {
            throw new LocalizedBadRequestException(LocalizedExceptionKeys.PERMIT_CONFLICTING);
        }
        permitEligibilityService.validateEligibility(currentUser, vehicle, area);

        Optional<String> orderId = payPalService.createPermitOrder(permitRequest);

        if (orderId.isEmpty())
            throw new LocalizedBadRequestException(LocalizedExceptionKeys.PAYMENT_CREATE_ORDER);

        permit.setPaypalId(orderId.get());
        permitService.save(permit);

        Map<String, String> response = new HashMap<>();
        response.put("orderId", orderId.get());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAdminPermit(
            @RequestBody PermitRequestDto permitRequest) {

        Vehicle vehicle = vehicleService.findById(permitRequest.vehicleId())
                .orElseThrow(() -> new LocalizedNotFoundException(LocalizedExceptionKeys.VEHICLE_NOT_FOUND, permitRequest.vehicleId()));

        Area area = areaService.findById(permitRequest.areaId())
                .orElseThrow(() -> new LocalizedNotFoundException(LocalizedExceptionKeys.AREA_NOT_FOUND, permitRequest.areaId()));

        User buyer = vehicle.getOwners().stream()
                .min(Comparator.comparing(User::getId))
                .orElseThrow(() -> new LocalizedNotFoundException(
                        LocalizedExceptionKeys.VEHICLE_NO_OWNERS));

        Permit permit = Mapper.mapPermitRequestToPermit(
                permitRequest, vehicle, area, buyer);

        if (permitService.permitsConflict(permit)) {
            throw new LocalizedBadRequestException(LocalizedExceptionKeys.PERMIT_CONFLICTING);
        }

        permitEligibilityService.validateEligibility(buyer, vehicle, area);

        permit.setPaymentTime(java.time.LocalDateTime.now());
        permit.setPaypalId(null);

        Permit savedPermit = permitService.save(permit);

        return HttpUtils.produceOkResponseWithBody(
                new PermitDto(savedPermit));
    }

    @PutMapping()
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> updatePermit(@RequestBody PermitUpdateForm permitDto){
        permitService.updatePermit(permitDto);
        return HttpUtils.produceOkResponse();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deletePermit(@PathVariable Long id){
        if(!permitService.existsById(id)){
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.PERMIT_NOT_FOUND, id);
        }
        permitService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }
}

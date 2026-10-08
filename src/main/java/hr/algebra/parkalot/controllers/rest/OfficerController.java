package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.*;
import hr.algebra.parkalot.models.dto.PermitCheckRequestDto;
import hr.algebra.parkalot.models.dto.PermitCheckResultDto;
import hr.algebra.parkalot.models.dto.PermitDto;
import hr.algebra.parkalot.models.dto.ViolationDto;
import hr.algebra.parkalot.models.entity.*;
import hr.algebra.parkalot.services.*;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.db.ParkingCheckLogService;
import hr.algebra.parkalot.services.db.ViolationService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/rest/officer")
@RequiredArgsConstructor
public class OfficerController {
    private final AuthUtils authUtils;
    private final ViolationService violationService;
    private final AreaService areaService;
    private final ParkingService parkingService;
    private final ParkingCheckLogService parkingCheckLogService;

    @GetMapping
    public ResponseEntity<?> checkVehicleStatus(@RequestBody PermitCheckRequestDto requestDto) {
        LocalDateTime timestamp = LocalDateTime.now();
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse("Unauthorized");
        } else if (currentUser.get().getRole() != Role.OFFICER) {
            return HttpUtils.produceForbiddenResponse("You are not an officer");
        }

        Area currentArea = areaService.getCurrentArea(
                requestDto.coordinates().latitude(),
                requestDto.coordinates().longitude()
        ).orElse(null);

        if (currentArea == null) {
            return HttpUtils.produceBadRequestResponse("Vehicle not in any zone");
        }

        Optional<Permit> permit = parkingService.checkParkingStatusIsValid(requestDto.licencePlate(), currentArea.getZone());
        ParkingCheckLog parkingLog = parkingCheckLogService.save(ParkingCheckLog.builder()
                .officer(currentUser.get())
                .latitude(requestDto.coordinates().latitude())
                .longitude(requestDto.coordinates().longitude())
                .timestamp(timestamp)
                .licencePlate(requestDto.licencePlate())
                .inViolation(permit.isEmpty())
                .resolvedArea(currentArea)
                .build());
        if (permit.isPresent()) {
            return ResponseEntity.ok(new PermitCheckResultDto(requestDto, timestamp, true,
                    new PermitDto(permit.get()), null));
        } else {
            Violation violation = violationService.issueViolation(currentUser.get(), currentArea, requestDto.licencePlate(), parkingLog);
            return ResponseEntity.ok(new PermitCheckResultDto(requestDto, timestamp, false,
                    null, new ViolationDto(violation)));
        }
    }
}

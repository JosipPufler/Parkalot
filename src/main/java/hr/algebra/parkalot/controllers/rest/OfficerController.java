package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.dtos.*;
import hr.algebra.parkalot.models.entities.*;
import hr.algebra.parkalot.services.*;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.db.ParkingCheckLogService;
import hr.algebra.parkalot.services.db.ViolationService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.io.ParseException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final AiService aiService;

    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> checkVehicleStatus(@RequestParam("image") MultipartFile image,
                                                @RequestParam("latitude") double latitude,
                                                @RequestParam("longitude") double longitude) throws ParseException, IOException {
        LocalDateTime timestamp = LocalDateTime.now();
        User currentUser = authUtils.requireCurrentUser();

        Area currentArea = areaService.getCurrentArea(
                latitude,
                longitude
        ).orElse(null);

        if (currentArea == null) {
            return HttpUtils.produceBadRequestResponse("Vehicle not in any zone");
        }

        String licencePlate = aiService.readLicencePlate(image);

        Optional<Permit> permit = parkingService.checkParkingStatusIsValid(licencePlate, currentArea);
        ParkingCheckLog parkingLog = parkingCheckLogService.save(ParkingCheckLog.builder()
                .officer(currentUser)
                .latitude(latitude)
                .longitude(longitude)
                .timestamp(timestamp)
                .licencePlate(licencePlate)
                .inViolation(permit.isEmpty())
                .resolvedArea(currentArea)
                .build());
        PermitCheckRequestDto simplifiedRequestDto = new PermitCheckRequestDto(licencePlate, new CoordinateDto(latitude, longitude));
        return generateParkingCheckResponse(timestamp, permit, parkingLog, simplifiedRequestDto);
    }

    private ResponseEntity<?> generateParkingCheckResponse(LocalDateTime timestamp, Optional<Permit> permit, ParkingCheckLog parkingLog, PermitCheckRequestDto simplifiedRequestDto) throws ParseException {
        if (permit.isPresent()) {
            return ResponseEntity.ok(new PermitCheckResultDto(simplifiedRequestDto, timestamp, true,
                    new PermitDto(permit.get()), null));
        } else {
            Violation violation = violationService.issueViolation(parkingLog);
            return ResponseEntity.ok(new PermitCheckResultDto(simplifiedRequestDto, timestamp, false,
                    null, new ViolationDto(violation)));
        }
    }
}

package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.*;
import hr.algebra.parkalot.models.dto.*;
import hr.algebra.parkalot.models.entity.*;
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

    @PostMapping
    public ResponseEntity<?> checkVehicleStatus(@RequestBody PermitCheckRequestDto requestDto) throws ParseException {
        LocalDateTime timestamp = LocalDateTime.now();
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse();
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

        Optional<Permit> permit = parkingService.checkParkingStatusIsValid(requestDto.licencePlate(), currentArea);
        ParkingCheckLog parkingLog = parkingCheckLogService.save(ParkingCheckLog.builder()
                .officer(currentUser.get())
                .latitude(requestDto.coordinates().latitude())
                .longitude(requestDto.coordinates().longitude())
                .timestamp(timestamp)
                .licencePlate(requestDto.licencePlate())
                .inViolation(permit.isEmpty())
                .resolvedArea(currentArea)
                .build());
        return generateParkingCheckResponse(timestamp, permit, parkingLog, requestDto);
    }

    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> checkVehicleStatus(@RequestParam("image") MultipartFile image,
                                                @RequestParam("latitude") double latitude,
                                                @RequestParam("longitude") double longitude) throws ParseException, IOException {
        LocalDateTime timestamp = LocalDateTime.now();
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse();
        // TODO CHANGE FROM ADMIN TO OFFICER
        } else if (currentUser.get().getRole() != Role.ADMIN) {
            return HttpUtils.produceForbiddenResponse("You are not an officer");
        }

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
                .officer(currentUser.get())
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

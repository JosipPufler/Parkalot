package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.Role;
import hr.algebra.parkalot.models.dto.AppealDto;
import hr.algebra.parkalot.models.dto.AppealCreationDto;
import hr.algebra.parkalot.models.dto.AppealEvidenceDto;
import hr.algebra.parkalot.models.entity.*;
import hr.algebra.parkalot.models.dto.ViolationDto;
import hr.algebra.parkalot.services.db.AppealEvidenceService;
import hr.algebra.parkalot.services.db.AppealService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import hr.algebra.parkalot.services.db.VehicleService;
import hr.algebra.parkalot.services.db.ViolationService;
import hr.algebra.parkalot.services.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/rest/violations")
@RequiredArgsConstructor
public class ViolationController {
    private final AuthUtils authUtils;
    private final ViolationService violationService;
    private final VehicleService vehicleService;
    private final AppealService appealService;
    private final AppealEvidenceService appealEvidenceService;

    @GetMapping
    public ResponseEntity<?> getViolations() {
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse();
        }

        List<Violation> violations = vehicleService.findByOwner(currentUser.get()).stream()
                .flatMap(vehicle -> violationService.findViolationsByLicencePlate(vehicle.getLicencePlate()).stream()).toList();
        return ResponseEntity.ok(violations.stream().map(ViolationDto::new).toList());
    }

    @GetMapping("/vehicle")
    public ResponseEntity<?> getVehicleViolations(@RequestParam String licensePlate) {
        return ResponseEntity.ok(violationService.findViolationsByLicencePlate(licensePlate).stream().map(ViolationDto::new));
    }

    @PostMapping()
    public ResponseEntity<?> resolveViolation(@RequestParam Long violationId){
        Optional<Violation> byId = violationService.findById(violationId);
        if (byId.isEmpty()) {
            return HttpUtils.produceNotFoundResponse("Violation not found");
        }

        Violation violation = byId.get();
        if (violation.getActive()) {
            violation.setActive(false);
            violationService.save(violation);
            return ResponseEntity.ok().build();
        }
        return HttpUtils.produceBadRequestResponse("Violation already resolved");
    }

    @PostMapping(path = "/appeal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> raiseViolationAppeal(@ModelAttribute AppealCreationDto appealCreationDto) throws IOException {
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse();
        }

        Optional<Violation> violation = violationService.findById(appealCreationDto.violationId());
        if (violation.isEmpty()) {
            return HttpUtils.produceNotFoundResponse("Violation not found");
        }
        Optional<Vehicle> vehicle = vehicleService.findByLicencePlate(violation.get().getParkingCheckLog().getLicencePlate());
        if (vehicle.isEmpty() || !Objects.equals(vehicle.get().getOwner().getId(), currentUser.get().getId())) {
            return HttpUtils.produceNotFoundResponse("Vehicle not found");
        }

        Appeal appeal = appealService.save(Mapper.mapAppealRequestToAppeal(appealCreationDto, vehicle.get(), violation.get()));
        AppealDto appealDto;
        if (!appealCreationDto.evidence().isEmpty())
            appealDto = new AppealDto(appeal,
                    appealService.constructAppealEvidence(appeal, appealCreationDto.evidence()).stream().map(AppealEvidenceDto::new).toList());
        else
             appealDto = new AppealDto(appeal);
        return ResponseEntity.ok(appealDto);
    }

    @GetMapping(path = "/appeal")
    public ResponseEntity<?> getAppeals() {
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isEmpty()) {
            return HttpUtils.produceUnauthorizedResponse();
        }

        if (currentUser.get().getRole() != Role.USER)
            return ResponseEntity.ok(appealService.findByOfficerId(currentUser.get().getId()).stream().map(AppealDto::new).toList());

        return ResponseEntity.ok(appealService.findByUserId(currentUser.get().getId()).stream().map(AppealDto::new).toList());
    }

    @PutMapping(path = "/appeal/resolve/{appealId}")
    public ResponseEntity<?> resolveAppeal(@PathVariable Long appealId) {
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isEmpty() || currentUser.get().getRole() == Role.USER) {
            return HttpUtils.produceUnauthorizedResponse();
        }

        Optional<Appeal> appeal = appealService.findById(appealId);
        if (appeal.isEmpty()) {
            return HttpUtils.produceNotFoundResponse("Appeal not found");
        }
        appealService.approveAppeal(appeal.get());

        return HttpUtils.produceOkResponse();
    }

    @PutMapping(path = "/appeal/reject/{appealId}")
    public ResponseEntity<?> rejectAppeal(@PathVariable Long appealId) {
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isEmpty() || currentUser.get().getRole() == Role.USER) {
            return HttpUtils.produceUnauthorizedResponse();
        }

        Optional<Appeal> appeal = appealService.findById(appealId);
        if (appeal.isEmpty()) {
            return HttpUtils.produceNotFoundResponse("Appeal not found");
        }
        appealService.rejectAppeal(appeal.get());

        return HttpUtils.produceOkResponse();
    }

    @GetMapping(path = "/appeal/{evidenceId}")
    public ResponseEntity<?> getAppealEvidence(@PathVariable Long evidenceId) {
        Optional<User> currentUser = authUtils.getCurrentUser();
        Optional<AppealEvidence> evidence = appealEvidenceService.findById(evidenceId);

        if (currentUser.isPresent() && evidence.isPresent() &&
                (Objects.equals(evidence.get().getAppeal().getUser().getId(), currentUser.get().getId())
                    || currentUser.get().getRole() == Role.OFFICER || currentUser.get().getRole() == Role.ADMIN
                )) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(evidence.get().getMediaType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(evidence.get().getFilename(),
                                            StandardCharsets.UTF_8).build().toString())
                    .body(evidence.get().getEvidence());
        }
        return HttpUtils.produceUnauthorizedResponse();
    }
}

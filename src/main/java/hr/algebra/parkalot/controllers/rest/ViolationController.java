package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.exceptions.LocalizedBadRequestException;
import hr.algebra.parkalot.exceptions.LocalizedConflictException;
import hr.algebra.parkalot.exceptions.LocalizedNotFoundException;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import hr.algebra.parkalot.models.enums.Role;
import hr.algebra.parkalot.models.dtos.*;
import hr.algebra.parkalot.models.entities.*;
import hr.algebra.parkalot.services.PayPalService;
import hr.algebra.parkalot.services.db.*;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import hr.algebra.parkalot.services.utils.Mapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    private final PayPalService payPalService;
    private final ViolationService violationService;
    private final VehicleService vehicleService;
    private final AppealService appealService;
    private final AppealEvidenceService appealEvidenceService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getViolations() {
        User currentUser = authUtils.requireCurrentUser();

        if (currentUser.getRole() == Role.ADMIN)
            return HttpUtils.produceOkResponseWithBody(violationService.findAll().stream().map(ViolationDto::new).toList());

        List<Violation> violations = vehicleService.findByOwner(currentUser).stream()
                .flatMap(vehicle -> violationService.findViolationsByLicencePlate(vehicle.getLicencePlate()).stream()).toList();
        return ResponseEntity.ok(violations.stream().map(ViolationDto::new).toList());
    }

    @GetMapping("/vehicle")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> getVehicleViolations(@RequestParam String licensePlate) {
        return ResponseEntity.ok(violationService.findViolationsByLicencePlate(licensePlate).stream().map(ViolationDto::new));
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> resolveViolation(@RequestParam Long violationId){
        User currentUser = authUtils.requireCurrentUser();

        Optional<Violation> byId = violationService.findById(violationId);
        if (byId.isEmpty()) {
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.VIOLATION_NOT_FOUND, violationId);
        }

        Optional<Vehicle> vehicle = vehicleService.findByLicencePlate(byId.get().getParkingCheckLog().getLicencePlate());

        if (vehicle.isEmpty() || !vehicle.get().isOwnedBy(currentUser)) {
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.VEHICLE_NOT_FOUND_PLATE, byId.get().getParkingCheckLog().getLicencePlate());
        }

        Violation violation = byId.get();
        if (violation.getActive()) {
            String violationPaymentOrder = payPalService.createViolationPaymentOrder(violation);
            violation.setOrderId(violationPaymentOrder);
            violationService.save(violation);
            return ResponseEntity.ok().build();
        }
        throw new LocalizedBadRequestException(LocalizedExceptionKeys.VIOLATION_ALREADY_RESOLVED);
    }

    @PutMapping()
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> updateViolation(@RequestBody ViolationUpdateForm violationUpdateForm) {
        violationService.updateViolation(violationUpdateForm);
        return HttpUtils.produceOkResponse();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteViolation(@PathVariable Long id){
        if(!violationService.existsById(id)){
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.VIOLATION_NOT_FOUND, id);
        }
        violationService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }

    @PostMapping(path = "/appeal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> raiseViolationAppeal(@ModelAttribute AppealCreationDto appealCreationDto) throws IOException {
        User currentUser = authUtils.requireCurrentUser();

        Optional<Violation> violation = violationService.findById(appealCreationDto.violationId());
        if (violation.isEmpty()) {
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.VIOLATION_NOT_FOUND, appealCreationDto.violationId());
        }
        Optional<Vehicle> vehicle = vehicleService.findByLicencePlate(violation.get().getParkingCheckLog().getLicencePlate());
        if (vehicle.isEmpty() || !vehicle.get().isOwnedBy(currentUser)) {
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.VEHICLE_NOT_FOUND_PLATE, violation.get().getParkingCheckLog().getLicencePlate());
        }

        Appeal appeal = appealService.save(Mapper.mapAppealRequestToAppeal(appealCreationDto, violation.get(), currentUser));
        AppealDto appealDto;
        if (!appealCreationDto.evidence().isEmpty())
            appealDto = new AppealDto(appeal,
                    appealService.constructAppealEvidence(appeal, appealCreationDto.evidence()).stream().map(AppealEvidenceDto::new).toList());
        else
             appealDto = new AppealDto(appeal);
        return ResponseEntity.ok(appealDto);
    }

    @PostMapping("/appeal/insert")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> raiseViolationAppeal(@ModelAttribute AdminAppealCreationDto appealCreationDto) throws IOException {
        User user = userService.findById(appealCreationDto.ownerId()).orElseThrow(() ->
            new LocalizedNotFoundException(LocalizedExceptionKeys.USER_NOT_FOUND, appealCreationDto.ownerId()));

        Violation violation = violationService.findById(appealCreationDto.violationId()).orElseThrow(() ->
                new LocalizedNotFoundException(LocalizedExceptionKeys.VIOLATION_NOT_FOUND, appealCreationDto.violationId()));

        Optional<Vehicle> vehicle = vehicleService.findByLicencePlate(violation.getParkingCheckLog().getLicencePlate());
        if (vehicle.isEmpty() || !vehicle.get().isOwnedBy(user)) {
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.VEHICLE_NOT_FOUND_PLATE, violation.getParkingCheckLog().getLicencePlate());
        }

        Appeal appeal = appealService.save(Mapper.mapAppealRequestToAppeal(appealCreationDto, violation, user));
        AppealDto appealDto;
        if (!appealCreationDto.evidence().isEmpty())
            appealDto = new AppealDto(appeal,
                    appealService.constructAppealEvidence(appeal, appealCreationDto.evidence()).stream().map(AppealEvidenceDto::new).toList());
        else
            appealDto = new AppealDto(appeal);
        return ResponseEntity.ok(appealDto);
    }

    @GetMapping(path = "/appeal")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> getAppeals() {
        User currentUser = authUtils.requireCurrentUser();

        List<Appeal> appeals = switch (currentUser.getRole()) {
            case USER -> appealService.findByUserId(currentUser.getId());
            case OFFICER -> appealService.findByOfficerId(currentUser.getId());
            case ADMIN -> appealService.findAll();
        };

        return ResponseEntity.ok(appeals.stream().map(AppealDto::new).toList());
    }

    @PutMapping(path = "/appeal/resolve/{appealId}")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<?> resolveAppeal(@PathVariable Long appealId) {
        Optional<Appeal> appeal = appealService.findById(appealId);
        if (appeal.isEmpty()) {
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.APPEAL_NOT_FOUND, appealId);
        }
        if (appeal.get().getResolved()) {
            throw new LocalizedConflictException(LocalizedExceptionKeys.APPEAL_ALREADY_RESOLVED);
        }

        appealService.approveAppeal(appeal.get());

        return HttpUtils.produceOkResponse();
    }

    @PutMapping(path = "/appeal/reject/{appealId}")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<?> rejectAppeal(@PathVariable Long appealId) {
        Optional<Appeal> appeal = appealService.findById(appealId);
        if (appeal.isEmpty()) {
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.APPEAL_NOT_FOUND, appealId);
        }
        if (appeal.get().getResolved()) {
            throw new LocalizedConflictException(LocalizedExceptionKeys.APPEAL_ALREADY_RESOLVED);
        }

        appealService.rejectAppeal(appeal.get());

        return HttpUtils.produceOkResponse();
    }

    @GetMapping(path = "/appeal/{evidenceId}")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> downloadAppealEvidence(@PathVariable Long evidenceId) {
        User currentUser = authUtils.requireCurrentUser();
        Optional<AppealEvidence> evidence = appealEvidenceService.findById(evidenceId);

        if (evidence.isPresent() &&
                (Objects.equals(evidence.get().getAppeal().getUser().getId(), currentUser.getId())
                    || currentUser.getRole() == Role.OFFICER || currentUser.getRole() == Role.ADMIN
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

    @GetMapping("/appeal/{appealId}/evidence")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<List<AppealEvidenceDto>> getAppealEvidence(
            @PathVariable Long appealId
    ) {
        User currentUser = authUtils.requireCurrentUser();

        Appeal appeal = appealService.findById(appealId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!Objects.equals(appeal.getUser().getId(), currentUser.getId())
                && currentUser.getRole() != Role.OFFICER
                && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<AppealEvidenceDto> evidence = appealEvidenceService
                .findByAppealId(appealId)
                .stream()
                .map(AppealEvidenceDto::new)
                .toList();

        return ResponseEntity.ok(evidence);
    }

    @PutMapping("/appeal")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> updateAppeal(@RequestBody AppealUpdateForm appealUpdateForm) {
        appealService.updateAppeal(appealUpdateForm);
        return HttpUtils.produceOkResponse();
    }

    @DeleteMapping("/appeal/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteAppeal(@PathVariable Long id){
        if(!appealService.existsById(id)){
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.APPEAL_NOT_FOUND, id);
        }
        appealService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }

    @DeleteMapping("/appeal/evidence/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteAppealEvidence(@PathVariable Long id){
        if(!appealEvidenceService.existsById(id)){
            throw new LocalizedNotFoundException(LocalizedExceptionKeys.EVIDENCE_NOT_FOUND, id);
        }
        appealEvidenceService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }
}

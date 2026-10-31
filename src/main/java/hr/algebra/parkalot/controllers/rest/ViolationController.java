package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.Role;
import hr.algebra.parkalot.models.dto.AppealDto;
import hr.algebra.parkalot.models.dto.AppealCreationDto;
import hr.algebra.parkalot.models.dto.AppealEvidenceDto;
import hr.algebra.parkalot.models.entity.*;
import hr.algebra.parkalot.models.dto.ViolationDto;
import hr.algebra.parkalot.services.PayPalService;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final PayPalService payPalService;
    private final ViolationService violationService;
    private final VehicleService vehicleService;
    private final AppealService appealService;
    private final AppealEvidenceService appealEvidenceService;

    @GetMapping
    public ResponseEntity<?> getViolations() {
        User currentUser = authUtils.requireCurrentUser();

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
            return HttpUtils.produceNotFoundResponse("Violation not found");
        }

        Optional<Vehicle> vehicle = vehicleService.findByLicencePlate(byId.get().getParkingCheckLog().getLicencePlate());

        if (vehicle.isEmpty() || !vehicle.get().isOwnedBy(currentUser)) {
            return HttpUtils.produceNotFoundResponse("Vehicle not found");
        }

        Violation violation = byId.get();
        if (violation.getActive()) {
            String violationPaymentOrder = payPalService.createViolationPaymentOrder(violation);
            violation.setOrderId(violationPaymentOrder);
            violationService.save(violation);
            return ResponseEntity.ok().build();
        }
        return HttpUtils.produceBadRequestResponse("Violation already resolved");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteViolation(@PathVariable Long id){
        if(!violationService.existsById(id)){
            return HttpUtils.produceNotFoundResponse("No violation for the given id: " + id);
        }
        violationService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }

    @PostMapping(path = "/appeal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<?> raiseViolationAppeal(@ModelAttribute AppealCreationDto appealCreationDto) throws IOException {
        User currentUser = authUtils.requireCurrentUser();

        Optional<Violation> violation = violationService.findById(appealCreationDto.violationId());
        if (violation.isEmpty()) {
            return HttpUtils.produceNotFoundResponse("Violation not found");
        }
        Optional<Vehicle> vehicle = vehicleService.findByLicencePlate(violation.get().getParkingCheckLog().getLicencePlate());
        if (vehicle.isEmpty() || !vehicle.get().isOwnedBy(currentUser)) {
            return HttpUtils.produceNotFoundResponse("Vehicle not found");
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

    @GetMapping(path = "/appeal")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> getAppeals() {
        User currentUser = authUtils.requireCurrentUser();

        if (currentUser.getRole() != Role.USER)
            return ResponseEntity.ok(appealService.findByOfficerId(currentUser.getId()).stream().map(AppealDto::new).toList());

        return ResponseEntity.ok(appealService.findByUserId(currentUser.getId()).stream().map(AppealDto::new).toList());
    }

    @PutMapping(path = "/appeal/resolve/{appealId}")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<?> resolveAppeal(@PathVariable Long appealId) {
        Optional<Appeal> appeal = appealService.findById(appealId);
        if (appeal.isEmpty()) {
            return HttpUtils.produceNotFoundResponse("Appeal not found");
        }
        appealService.approveAppeal(appeal.get());

        return HttpUtils.produceOkResponse();
    }

    @PutMapping(path = "/appeal/reject/{appealId}")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<?> rejectAppeal(@PathVariable Long appealId) {
        Optional<Appeal> appeal = appealService.findById(appealId);
        if (appeal.isEmpty()) {
            return HttpUtils.produceNotFoundResponse("Appeal not found");
        }
        appealService.rejectAppeal(appeal.get());

        return HttpUtils.produceOkResponse();
    }

    @GetMapping(path = "/appeal/{evidenceId}")
    @PreAuthorize("hasAnyRole('USER', 'OFFICER', 'ADMIN')")
    public ResponseEntity<?> getAppealEvidence(@PathVariable Long evidenceId) {
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

    @DeleteMapping("/appeal/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteAppeal(@PathVariable Long id){
        if(!appealService.existsById(id)){
            return HttpUtils.produceNotFoundResponse("No appeal for the given id: " + id);
        }
        appealService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }

    @DeleteMapping("/appeal/evidence/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteAppealEvidence(@PathVariable Long id){
        if(!appealEvidenceService.existsById(id)){
            return HttpUtils.produceNotFoundResponse("No appeal evidence for the given id: " + id);
        }
        appealEvidenceService.deleteById(id);
        return HttpUtils.produceOkResponse();
    }
}

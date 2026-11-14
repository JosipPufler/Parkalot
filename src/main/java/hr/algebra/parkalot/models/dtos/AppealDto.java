package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.entities.Appeal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AppealDto(Long appealId, List<AppealEvidenceDto> evidenceId, String title, String description, Long violationId,
                        LocalDate deadline, LocalDateTime issuedAt, Boolean resolved, Boolean rejected, String licencePlate) {
    public AppealDto(Appeal appeal) {
        this(appeal.getId(), appeal.getEvidence().stream().map(AppealEvidenceDto::new).toList(), appeal.getTitle(),
                appeal.getDescription(), appeal.getViolation().getId(), appeal.getDeadline(), appeal.getCreatedAt(),
                appeal.getResolved(), appeal.getRejected(), appeal.getViolation().getParkingCheckLog().getLicencePlate());
    }

    public AppealDto(Appeal appeal, List<AppealEvidenceDto> evidence) {
        this(appeal.getId(), evidence, appeal.getTitle(),
                appeal.getDescription(), appeal.getViolation().getId(), appeal.getDeadline(), appeal.getCreatedAt(),
                appeal.getResolved(), appeal.getRejected(), appeal.getViolation().getParkingCheckLog().getLicencePlate());
    }
}

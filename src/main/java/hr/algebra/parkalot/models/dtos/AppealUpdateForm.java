package hr.algebra.parkalot.models.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppealUpdateForm(Long id, String title, String description, Long violationId,
                               LocalDate deadline, LocalDateTime issuedAt, Boolean resolved) {
}

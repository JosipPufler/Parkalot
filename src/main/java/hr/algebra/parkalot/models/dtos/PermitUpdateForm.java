package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.enums.PermitDuration;

import java.time.LocalDate;

public record PermitUpdateForm(Long id, LocalDate startDate, PermitDuration permitDuration, Long areaId, Long vehicleId) {
}

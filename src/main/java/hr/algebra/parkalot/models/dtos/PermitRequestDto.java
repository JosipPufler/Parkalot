package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.enums.PermitDuration;

import java.time.LocalDate;

public record PermitRequestDto(Long vehicleId, Long areaId, LocalDate startDate, PermitDuration permitDuration) {}

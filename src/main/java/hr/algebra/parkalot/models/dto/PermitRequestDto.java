package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.PermitDuration;

import java.time.LocalDate;

public record PermitRequestDto(Long vehicleId, Long areaId, LocalDate startDate, PermitDuration permitDuration) {}

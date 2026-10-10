package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.PermitDuration;
import hr.algebra.parkalot.models.Zone;

import java.time.LocalDate;

public record PermitRequestDto(Long vehicleId, Zone zone, LocalDate startDate, PermitDuration permitDuration) {}

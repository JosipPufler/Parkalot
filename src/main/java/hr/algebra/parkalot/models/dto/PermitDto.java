package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.Permit;
import hr.algebra.parkalot.models.entity.PermitDuration;
import hr.algebra.parkalot.models.Zone;

import java.time.LocalDate;

public record PermitDto(LocalDate startDate, PermitDuration permitDuration, LocalDate endDate, Boolean expired,
                        Boolean active, Double price, Zone zone, VehicleDto vehicle) {
    public PermitDto(Permit permit) {
        this(permit.getStartDate(), permit.getPermitDuration(), permit.getExpirationDate(), permit.isExpired(),
                permit.isActive(), permit.getPrice(), permit.getZone(), new VehicleDto(permit.getVehicle()));
    }
}

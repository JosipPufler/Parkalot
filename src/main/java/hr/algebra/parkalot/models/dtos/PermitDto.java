package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.enums.PermitDuration;
import hr.algebra.parkalot.models.entities.Permit;

import java.time.LocalDate;

public record PermitDto(Long id, LocalDate startDate, PermitDuration permitDuration, LocalDate endDate, Boolean expired,
                        Boolean active, Double price, AreaDto areaDto, VehicleDto vehicleDto, Long userId, String email) {
    public PermitDto(Permit permit) {
        this(permit.getId(), permit.getStartDate(), permit.getPermitDuration(), permit.getExpirationDate(), permit.isExpired(),
                permit.isActive(), permit.getPrice(), new AreaDto(permit.getArea()), new VehicleDto(permit.getVehicle()),
                permit.getBuyer().getId(), permit.getBuyer().getEmail());
    }
}

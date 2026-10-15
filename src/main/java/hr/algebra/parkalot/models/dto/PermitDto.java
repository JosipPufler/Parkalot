package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.PermitDuration;
import hr.algebra.parkalot.models.entity.Permit;
import org.locationtech.jts.io.ParseException;

import java.time.LocalDate;

public record PermitDto(LocalDate startDate, PermitDuration permitDuration, LocalDate endDate, Boolean expired,
                        Boolean active, Double price, AreaDto areaDto, VehicleDto vehicle) {
    public PermitDto(Permit permit) throws ParseException {
        this(permit.getStartDate(), permit.getPermitDuration(), permit.getExpirationDate(), permit.isExpired(),
                permit.isActive(), permit.getPrice(), new AreaDto(permit.getArea()), new VehicleDto(permit.getVehicle()));
    }
}

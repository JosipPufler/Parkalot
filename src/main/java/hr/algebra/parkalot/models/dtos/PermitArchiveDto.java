package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.entities.PermitArchive;
import hr.algebra.parkalot.models.enums.Zone;

import java.time.LocalDate;

public record PermitArchiveDto(Zone zone, Double price, String username, String licencePlateNumber, LocalDate startTime, LocalDate endTime) {
    public PermitArchiveDto(PermitArchive permit) {
        this(permit.getZone(), permit.getPrice(), permit.getUsername(), permit.getLicencePlate(), permit.getStartDate(), permit.getEndDate());
    }
}

package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.PermitArchive;
import hr.algebra.parkalot.models.Zone;

import java.time.LocalDate;

public record PermitArchiveDto(Zone zone, Double price, String username, String licencePlateNumber, LocalDate startTime, LocalDate endTime) {
    public PermitArchiveDto(PermitArchive permit) {
        this(permit.getZone(), permit.getPrice(), permit.getUsername(), permit.getLicencePlate(), permit.getStartDate(), permit.getEndDate());
    }
}

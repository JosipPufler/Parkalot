package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.Violation;

import java.time.LocalDateTime;

public record ViolationDto(Long id, String licencePlate, String message, CoordinateDto coordinates, String area,
                           String officer, LocalDateTime issuedAt, Double price, Boolean active) {
    public ViolationDto(Violation violation) {
        this(violation.getId(), violation.getParkingCheckLog().getLicencePlate(), violation.getMessage(),
                new CoordinateDto(violation.getParkingCheckLog().getLatitude(), violation.getParkingCheckLog().getLongitude()),
                violation.getParkingCheckLog().getResolvedArea().getName(), violation.getParkingCheckLog().getOfficer().getName(),
                violation.getTimestamp(), violation.getPrice(), violation.getActive());
    }
}

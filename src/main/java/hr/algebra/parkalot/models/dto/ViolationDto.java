package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.Violation;

import java.time.LocalDateTime;

public record ViolationDto(Long id, String licencePlate, String message, CoordinateDto coordinates, String area,
                           String officer, LocalDateTime issuedAt, Double price, Boolean active) {
    public ViolationDto(Violation violation) {
        this(violation.getId(), violation.getLicencePlate(), violation.getMessage(),
                new CoordinateDto(violation.getLatitude(), violation.getLongitude()),
                violation.getArea().getName(), violation.getOfficer().getName(),
                violation.getTimestamp(), violation.getPrice(), violation.getActive());
    }
}

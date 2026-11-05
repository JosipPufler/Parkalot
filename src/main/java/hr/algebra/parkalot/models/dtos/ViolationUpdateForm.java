package hr.algebra.parkalot.models.dtos;

import java.time.LocalDateTime;

public record ViolationUpdateForm(Long id, String licencePlate, String message, CoordinateDto coordinates, Long areaId,
                                  Long officerId, LocalDateTime issuedAt, Double price, Boolean active) {
}

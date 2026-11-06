package hr.algebra.parkalot.models.dtos;

import java.util.List;

public record VehicleUpdateForm(Long id, String make, String model, int year, String licencePlate, List<Long> ownerIds) {
}

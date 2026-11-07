package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;

import java.util.List;

public record VehicleDto(Long id, String make, String model, int year, String licencePlate, List<Long> ownerIds) {
    public VehicleDto(Vehicle vehicle) {
        this(vehicle.getId(), vehicle.getMake(), vehicle.getModel(), vehicle.getYear(), vehicle.getLicencePlate(),
                vehicle.getOwners().stream().map(User::getId).toList());
    }
}

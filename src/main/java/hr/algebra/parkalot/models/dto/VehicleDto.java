package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.Vehicle;

public record VehicleDto(String make, String model, int year, String licencePlate) {
    public VehicleDto(Vehicle vehicle) {
        this(vehicle.getMake(), vehicle.getModel(), vehicle.getYear(), vehicle.getLicencePlate());
    }
}

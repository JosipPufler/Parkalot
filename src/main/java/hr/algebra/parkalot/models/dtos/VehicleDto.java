package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.models.enums.EmissionClass;

import java.util.List;

public record VehicleDto(
        Long id, String make, String model, int year, String licencePlate, List<Long> ownerIds, EmissionClass emissionClass
) {
    public VehicleDto(Vehicle vehicle) {
        this(
                vehicle.getId(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getLicencePlate(),
                vehicle.getOwnerships().stream().map(x -> x.getUser().getId()).toList(),
                vehicle.getEmissionClass()
        );
    }
}

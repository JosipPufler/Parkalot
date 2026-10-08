package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.Vehicle;

import java.io.Serializable;
import java.util.List;

public record VehicleDetailsDto(VehicleDto vehicle, List<PermitDto> permits, List<PermitArchiveDto> pastPermits) implements Serializable {
    public VehicleDetailsDto(Vehicle vehicle, List<PermitArchiveDto> pastPermits) {
        this(new VehicleDto(vehicle), vehicle.getPermits().stream().map(PermitDto::new).toList(), pastPermits);
    }
}

package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.Permit;
import hr.algebra.parkalot.models.entity.Vehicle;
import org.locationtech.jts.io.ParseException;

import java.io.Serializable;
import java.util.List;

public record VehicleDetailsDto(VehicleDto vehicle, List<PermitDto> permits, List<PermitArchiveDto> pastPermits) implements Serializable {
    public VehicleDetailsDto(Vehicle vehicle, List<PermitArchiveDto> pastPermits) throws ParseException {
        this(new VehicleDto(vehicle), null, pastPermits);

        for (Permit permit : vehicle.getPermits()) {
            permits.add(new PermitDto(permit));
        }

    }
}

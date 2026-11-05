package hr.algebra.parkalot.models.dtos;

import hr.algebra.parkalot.models.entities.Area;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AreaDto(Long id, String name, String zone, @Size(min = 3) List<CoordinateDto> coordinates) {
    public AreaDto(Area area) {
        this(area.getId(), area.getName(), area.getZone().getName(), area.getCoordinates());
    }
}

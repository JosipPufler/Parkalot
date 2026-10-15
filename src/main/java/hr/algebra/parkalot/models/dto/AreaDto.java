package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.Area;
import jakarta.validation.constraints.Size;
import org.locationtech.jts.io.ParseException;

import java.util.List;

public record AreaDto(Long id, String name, String zone, @Size(min = 3) List<CoordinateDto> coordinates) {
    public AreaDto(Area area) throws ParseException {
        this(area.getId(), area.getName(), area.getZone().getName(), area.getCoordinates());
    }
}

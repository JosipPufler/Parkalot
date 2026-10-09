package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.Area;
import org.locationtech.jts.io.ParseException;

import java.util.List;

public record AreaDto(String name, String zone, List<CoordinateDto> coordinates) {
    public AreaDto(Area area) throws ParseException {
        this(area.getName(), area.getZone().getName(), area.getCoordinates());
    }
}

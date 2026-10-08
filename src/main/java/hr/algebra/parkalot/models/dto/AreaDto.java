package hr.algebra.parkalot.models.dto;

import hr.algebra.parkalot.models.entity.Area;
import hr.algebra.parkalot.models.Zone;

import java.util.List;

public record AreaDto(String name, Zone zone, List<CoordinateDto> coordinates) {
    public AreaDto(Area area) {
        this(area.getName(), area.getZone(), area.getAreaPolygon().getPoints().stream().map(x -> new CoordinateDto(x.getX(), x.getY())).toList());
    }
}

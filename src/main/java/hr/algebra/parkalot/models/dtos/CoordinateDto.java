package hr.algebra.parkalot.models.dtos;

import org.locationtech.jts.geom.Coordinate;

public record CoordinateDto(Double latitude, Double longitude) {
    public CoordinateDto(Coordinate coordinate) {
        this(coordinate.y, coordinate.x);
    }
}

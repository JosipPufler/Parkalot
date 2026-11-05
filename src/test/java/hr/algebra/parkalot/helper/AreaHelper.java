package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.models.enums.Zone;
import hr.algebra.parkalot.models.entities.Area;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AreaHelper {
    public static Area getArea(){
        List<Coordinate> coordinates = List.of(new Coordinate(0, 0), new Coordinate(0, 1),
                new Coordinate(1, 1), new Coordinate(1, 0), new Coordinate(0, 0));

        Polygon polygon = new GeometryFactory().createPolygon(coordinates.toArray(new Coordinate[0]));

        return Area.builder()
                .name("Area1")
                .areaPolygon(polygon)
                .zone(Zone.ZONE_1)
                .build();
    }

    public static Area getAreaWithId(){
        Area area = getArea();
        area.setId(1L);
        return area;
    }
}

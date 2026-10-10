package hr.algebra.parkalot.models.entity;

import hr.algebra.parkalot.models.Zone;
import hr.algebra.parkalot.models.dto.CoordinateDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.springframework.data.geo.Polygon;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "areas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;
    @NotNull
    private byte[] areaPolygon;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Zone zone;

    @OneToMany(mappedBy = "resolvedArea", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkingCheckLog> parkingCheckLogs;

    public List<CoordinateDto> getCoordinates() throws ParseException {
        Geometry geometry = new WKBReader(new GeometryFactory()).read(areaPolygon);

        return Arrays.stream(geometry.getCoordinates())
                .map(c -> new CoordinateDto(c.y, c.x))
                .toList();
    }
}

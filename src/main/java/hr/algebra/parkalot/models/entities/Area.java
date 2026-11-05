package hr.algebra.parkalot.models.entities;

import hr.algebra.parkalot.models.enums.Zone;
import hr.algebra.parkalot.models.dtos.CoordinateDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
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
    @Column(unique = true)
    private String name;
    @Column(name = "area_polygon", columnDefinition = "geometry(Polygon, 4326)", nullable = false)
    private Polygon areaPolygon;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Zone zone;

    @OneToMany(mappedBy = "resolvedArea", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParkingCheckLog> parkingCheckLogs = new ArrayList<>();
    @OneToMany(mappedBy = "area", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Permit> permits = new ArrayList<>();

    public List<CoordinateDto> getCoordinates() {
        return Arrays.stream(areaPolygon.getCoordinates()).map(CoordinateDto::new).toList();
    }
}

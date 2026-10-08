package hr.algebra.parkalot.models.entity;

import hr.algebra.parkalot.models.Zone;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.geo.Polygon;

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
    private Polygon areaPolygon;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Zone zone;

    @OneToMany
    private List<Violation> violations;
}

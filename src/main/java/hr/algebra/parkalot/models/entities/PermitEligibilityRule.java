package hr.algebra.parkalot.models.entities;

import hr.algebra.parkalot.models.enums.EmissionClass;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "permit_eligibility_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermitEligibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "area_id", nullable = false, unique = true)
    private Area area;

    @NotNull
    @Builder.Default
    private Boolean requiresResidentAddress = false;

    private Integer maxVehiclesPerHousehold;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EmissionClass minimumEmissionClass;
}

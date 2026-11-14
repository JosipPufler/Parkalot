package hr.algebra.parkalot.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "vehicle_ownerships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_vehicle_ownership",
                        columnNames = {"user_id", "vehicle_id"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleOwnership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    @NotNull
    private LocalDate ownershipStartDate;
    private LocalDate ownershipEndDate;
    @NotNull
    private Boolean primaryOwner;
}
package hr.algebra.parkalot.models.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "violations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingCheckLog {
    @Id
    private Long id;

    @NotNull
    private LocalDateTime timestamp;
    @NotNull
    private Double longitude;
    @NotNull
    private Double latitude;
    @NotBlank
    private String licencePlate;
    @NotNull
    private Boolean inViolation;

    @ManyToOne
    private Area resolvedArea;
    @ManyToOne
    private User officer;
}

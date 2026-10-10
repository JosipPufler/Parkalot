package hr.algebra.parkalot.models.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "parkingLogs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingCheckLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @JoinColumn(name = "area_id", nullable = false)
    private Area resolvedArea;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User officer;
    @OneToOne(optional = false)
    private Violation violation;
}

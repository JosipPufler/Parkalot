package hr.algebra.parkalot.models.entity;

import jakarta.persistence.*;
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
public class Violation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDateTime timestamp;
    @NotNull
    private String message;
    @NotNull
    private Double price;
    @NotBlank
    private String licencePlate;
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
    @NotNull
    private Boolean active;

    @ManyToOne(optional = false)
    private Area area;
    @ManyToOne(optional = false)
    private User officer;
    @OneToOne(optional = false)
    private ParkingCheckLog parkingCheckLog;
}

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
    @NotNull
    private Boolean active;

    @OneToOne(optional = false)
    private ParkingCheckLog parkingCheckLog;
}

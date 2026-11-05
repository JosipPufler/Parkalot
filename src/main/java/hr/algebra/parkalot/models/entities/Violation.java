package hr.algebra.parkalot.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
    @Column(unique = true, nullable = true)
    private String orderId;
    private LocalDateTime paymentTime;

    @OneToOne(optional = false)
    private ParkingCheckLog parkingCheckLog;

    @OneToMany(mappedBy = "violation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appeal> appeals;
}

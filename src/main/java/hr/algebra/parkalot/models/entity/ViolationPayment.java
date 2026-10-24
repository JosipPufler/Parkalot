package hr.algebra.parkalot.models.entity;

import hr.algebra.parkalot.models.PermitDuration;
import hr.algebra.parkalot.models.Zone;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "violationPayment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    LocalDateTime createdAt;
    @NotNull
    LocalDateTime paymentTime;
    @NotBlank
    String paypalId;
    @NotNull
    Double expectedPrice;

    @OneToOne(optional = false)
    private Violation violation;
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

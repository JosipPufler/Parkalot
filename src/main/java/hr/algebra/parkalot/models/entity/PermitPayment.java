package hr.algebra.parkalot.models.entity;


import hr.algebra.parkalot.models.PermitDuration;
import hr.algebra.parkalot.models.Zone;
import hr.algebra.parkalot.models.dto.PermitDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "permitPayment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermitPayment {
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
    @Enumerated(EnumType.STRING)
    PermitDuration permitDuration;
    @NotNull
    @Enumerated(EnumType.STRING)
    Zone zone;
    @NotNull
    Double expectedPrice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

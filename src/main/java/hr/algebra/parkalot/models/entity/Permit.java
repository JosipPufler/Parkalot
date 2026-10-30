package hr.algebra.parkalot.models.entity;

import hr.algebra.parkalot.models.PermitDuration;
import hr.algebra.parkalot.models.Zone;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "permits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private LocalDate startDate;
    @NotNull
    @Enumerated(EnumType.STRING)
    private PermitDuration permitDuration;
    @NotNull
    private Double price;
    @NotNull
    LocalDateTime createdAt;
    LocalDateTime paymentTime;
    @NotBlank
    String paypalId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    @ManyToOne(optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    public LocalDate getExpirationDate() {
        return permitDuration.calculateEndDate.apply(startDate);
    }

    public Boolean isExpired(){
        if (LocalDate.now().isBefore(startDate))
            return false;

        LocalDate endDate = getExpirationDate();

        return LocalDate.now().isAfter(endDate);
    }

    public Boolean isActive() {
        return (LocalDate.now().isAfter(startDate) || LocalDate.now().isEqual(startDate)) && !isExpired() && paymentTime != null;
    }

    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return !getExpirationDate().isBefore(otherStart) && !otherEnd.isBefore(getStartDate());
    }
}

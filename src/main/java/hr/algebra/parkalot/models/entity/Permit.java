package hr.algebra.parkalot.models.entity;

import hr.algebra.parkalot.models.Zone;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

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
    @Enumerated(EnumType.STRING)
    private Zone zone;

    @ManyToOne(optional = false)
    private Vehicle vehicle;

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
        return LocalDate.now().isAfter(startDate) && !isExpired();
    }

    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return !getExpirationDate().isBefore(otherStart) && !otherEnd.isBefore(getStartDate());
    }
}

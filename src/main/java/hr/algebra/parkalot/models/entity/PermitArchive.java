package hr.algebra.parkalot.models.entity;

import hr.algebra.parkalot.models.Zone;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "permitArchives")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermitArchive {
    public PermitArchive(Permit permit){
        this.startDate = permit.getStartDate();
        this.endDate = permit.getExpirationDate();
        this.areaName = permit.getArea().getName();
        this.zone = permit.getArea().getZone();
        this.user = permit.getBuyer();
        this.username = this.user.getFirstName() + " " + this.user.getLastName();
        this.email = this.user.getEmail();
        this.licencePlate = permit.getVehicle().getLicencePlate();
        this.price = permit.getPrice();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String areaName;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Zone zone;
    @NotBlank
    private String licencePlate;
    @NotBlank
    private String username;
    @NotBlank
    private String email;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    @NotNull
    private Double price;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}

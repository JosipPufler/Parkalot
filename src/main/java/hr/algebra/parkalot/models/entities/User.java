package hr.algebra.parkalot.models.entities;

import hr.algebra.parkalot.models.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String firstName;
    private String lastName;
    @NotBlank
    private String password;
    @NotBlank
    @Column(unique = true)
    private String email;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<VehicleOwnership> vehicleOwnerships = new ArrayList<>();

    @OneToMany(mappedBy = "officer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkingCheckLog> parkingCheckLogs;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appeal> appeals;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PermitArchive> permitArchives;

    @ManyToOne
    @JoinColumn(name = "household_id")
    private Household household;

    public String getName(){
        return lastName == null ? firstName : firstName + " " + lastName;
    }

    public void addVehicleOwnership(VehicleOwnership ownership) {
        vehicleOwnerships.add(ownership);
        ownership.setUser(this);
    }
}

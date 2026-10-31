package hr.algebra.parkalot.models.entity;

import hr.algebra.parkalot.models.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @ManyToMany
    @JoinTable(
            name = "user_vehicles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "vehicle_id")
    )
    private List<Vehicle> vehicles = new ArrayList<>();

    @OneToMany(mappedBy = "officer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkingCheckLog> parkingCheckLogs;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appeal> appeals;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PermitArchive> permitArchives;

    public String getName(){
        return lastName == null ? firstName : firstName + " " + lastName;
    }
}

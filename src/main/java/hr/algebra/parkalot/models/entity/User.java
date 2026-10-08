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
    @NotBlank
    private String lastName;
    @NotBlank
    private String password;
    @NotBlank
    @Column(unique = true)
    private String email;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany
    private List<Vehicle> vehicles = new ArrayList<>();

    public String getName(){
        return firstName + " " + lastName;
    }
}

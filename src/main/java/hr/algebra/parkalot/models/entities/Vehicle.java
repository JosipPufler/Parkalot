package hr.algebra.parkalot.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String make;
    @NotBlank
    private String model;
    @NotNull
    private int year;
    @NotBlank
    @Column(unique = true)
    private String licencePlate;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Permit> permits = new ArrayList<>();

    @ManyToMany(mappedBy = "vehicles")
    @Builder.Default
    private List<User> owners = new ArrayList<>();

    public Boolean isOwnedBy(User user) {
        return owners.stream().anyMatch(owner -> owner.getId().equals(user.getId()));
    }
}

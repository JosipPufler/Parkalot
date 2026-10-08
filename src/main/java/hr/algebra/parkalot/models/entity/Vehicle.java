package hr.algebra.parkalot.models.entity;

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
public class Vehicle {
    @Id
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

    @OneToMany
    private List<Permit> permits = new ArrayList<>();
    @ManyToOne(optional = false)
    private User user;
}

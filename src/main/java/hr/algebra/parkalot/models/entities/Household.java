package hr.algebra.parkalot.models.entities;

import hr.algebra.parkalot.models.dtos.HouseholdDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "households")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Household {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String street;
    @NotBlank
    private String houseNumber;
    @NotBlank
    private String city;
    @NotBlank
    private String country;
    @NotBlank
    private String postalCode;
    @Column(
            name = "location",
            columnDefinition = "geometry(Point, 4326)"
    )
    private Point location;

    @OneToMany(mappedBy = "household")
    @Builder.Default
    private List<User> members = new ArrayList<>();

    public Boolean differs(HouseholdDto householdDto){
        return !Objects.equals(street, householdDto.street()) || !Objects.equals(houseNumber, householdDto.houseNumber())
                || !Objects.equals(country, householdDto.country()) || !Objects.equals(city, householdDto.city())
                || !Objects.equals(postalCode, householdDto.postalCode());
    }
}

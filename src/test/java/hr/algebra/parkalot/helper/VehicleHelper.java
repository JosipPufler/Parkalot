package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VehicleHelper {
    public static Vehicle getVehicle(User owner) {
        return Vehicle.builder()
                .year(2000)
                .licencePlate("ZG1234AB")
                .make("make")
                .model("model")
                .owners(List.of(owner))
                .build();
    }
}

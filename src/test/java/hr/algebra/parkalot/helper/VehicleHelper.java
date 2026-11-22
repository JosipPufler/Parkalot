package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Vehicle;
import hr.algebra.parkalot.models.entities.VehicleOwnership;
import hr.algebra.parkalot.models.enums.EmissionClass;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VehicleHelper {
    public static Vehicle getVehicle(User owner) {
        Vehicle vehicle = Vehicle.builder()
                .year(2000)
                .licencePlate("ZG1234AB")
                .make("make")
                .model("model")
                .emissionClass(EmissionClass.EURO_6)
                .build();
        VehicleOwnership ownership = VehicleOwnership.builder()
                .user(owner)
                .vehicle(vehicle)
                .ownershipStartDate(LocalDate.now())
                .primaryOwner(true)
                .build();
        vehicle.getOwnerships().add(ownership);
        owner.getVehicleOwnerships().add(ownership);
        return vehicle;
    }
}

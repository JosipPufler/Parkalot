package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.models.entity.Area;
import hr.algebra.parkalot.models.entity.ParkingCheckLog;
import hr.algebra.parkalot.models.entity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParkingCheckLogHelper {
    public static ParkingCheckLog getParkingCheckLogInViolation(User officer, String licencePlate, Area area) {
        return ParkingCheckLog.builder()
                .timestamp(LocalDateTime.now())
                .officer(officer)
                .resolvedArea(area)
                .inViolation(true)
                .licencePlate(licencePlate)
                .latitude(0.0)
                .longitude(0.0)
                .build();
    }

    public static ParkingCheckLog getParkingCheckLogNotInViolation(User officer, String licencePlate, Area area) {
        ParkingCheckLog parkingCheckLog = getParkingCheckLogInViolation(officer, licencePlate, area);
        parkingCheckLog.setInViolation(false);
        return parkingCheckLog;
    }
}

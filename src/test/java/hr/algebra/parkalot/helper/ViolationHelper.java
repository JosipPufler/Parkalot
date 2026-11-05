package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.models.entities.Area;
import hr.algebra.parkalot.models.entities.ParkingCheckLog;
import hr.algebra.parkalot.models.entities.User;
import hr.algebra.parkalot.models.entities.Violation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ViolationHelper {
    public static Violation getViolation(User officer, String licencePlate, Area area){
        ParkingCheckLog parkingCheckLog = ParkingCheckLogHelper.getParkingCheckLogInViolation(officer, licencePlate, area);

        Violation violation = Violation.builder()
                .active(true)
                .message("Sample message")
                .parkingCheckLog(parkingCheckLog)
                .price(16.99)
                .build();

        parkingCheckLog.setViolation(violation);
        return violation;
    }
}

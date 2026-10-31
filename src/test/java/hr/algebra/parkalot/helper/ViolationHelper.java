package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.models.entity.Area;
import hr.algebra.parkalot.models.entity.ParkingCheckLog;
import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.models.entity.Violation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

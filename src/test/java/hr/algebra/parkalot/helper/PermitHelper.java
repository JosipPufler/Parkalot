package hr.algebra.parkalot.helper;

import hr.algebra.parkalot.models.PermitDuration;
import hr.algebra.parkalot.models.entity.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PermitHelper {
    public static Permit getPermit(User user, Vehicle vehicle, Area area) {
        return Permit.builder()
                .permitDuration(PermitDuration.DAY)
                .buyer(user)
                .vehicle(vehicle)
                .area(area)
                .startDate(LocalDate.now())
                .paymentTime(LocalDateTime.now())
                .price(7.99)
                .build();
    }

    public static PermitArchive getPermitArchive(Permit permit) {
        return new PermitArchive(permit);
    }
}

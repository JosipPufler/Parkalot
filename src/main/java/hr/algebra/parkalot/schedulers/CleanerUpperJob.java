package hr.algebra.parkalot.schedulers;

import hr.algebra.parkalot.services.db.ParkingCheckLogService;
import hr.algebra.parkalot.services.db.PermitService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CleanerUpperJob {
    private final PermitService permitService;
    private final ParkingCheckLogService parkingCheckLogService;

    @Scheduled(cron = "0 7 1 * * *")
    public void reportCurrentTime() {
        permitService.cleanUpExpired();
        parkingCheckLogService.cleanup();
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void clearUnpaidPermits(){
        permitService.cleanUpUnpaid();
    }
}

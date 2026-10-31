package hr.algebra.parkalot.services.db;

import hr.algebra.parkalot.models.entity.ParkingCheckLog;
import hr.algebra.parkalot.repositories.ParkingCheckLogRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class ParkingCheckLogService extends GeneralCrudService<ParkingCheckLog, ParkingCheckLogRepo> {
    protected ParkingCheckLogService(ParkingCheckLogRepo repository) {
        super(repository);
    }

    @Transactional
    public void cleanup(){
        findAll().forEach(log -> {
            if (log.getViolation() != null
                    && ChronoUnit.MONTHS.between(log.getTimestamp(), LocalDateTime.now()) >= 1) {
                delete(log);
            }
        });
    }
}
